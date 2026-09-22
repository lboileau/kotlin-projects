package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.FindByWebLinkParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import com.acme.services.camperservice.features.recipe.params.ImportRecipeParam
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Fetches the HTML content of a URL. Throws on failure. */
fun interface HtmlFetcher {
    fun fetch(url: String): String
}

/** Default implementation using Java's built-in HttpClient. */
fun defaultHtmlFetcher(): HtmlFetcher {
    val httpClient = HttpClient.newHttpClient()
    return HtmlFetcher { url ->
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0 (compatible; CamperBot/1.0)")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("HTTP ${response.statusCode()}")
        }
        response.body()
    }
}

internal class ImportRecipeAction(
    private val recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    private val recipeScraperClient: RecipeScraperClient,
    photoStore: RecipePhotoStore,
    private val htmlFetcher: HtmlFetcher = defaultHtmlFetcher()
) {
    private val logger = LoggerFactory.getLogger(ImportRecipeAction::class.java)
    private val drafter = ScrapedRecipeDrafter(recipeClient, ingredientClient)
    private val getRecipe = GetRecipeAction(recipeClient, ingredientClient, photoStore)

    fun execute(param: ImportRecipeParam): Result<RecipeDetailResponse, RecipeError> {
        if (param.url.isBlank()) {
            return Result.Failure(RecipeError.Invalid("url", "must not be blank"))
        }

        // Check URL not already imported
        when (val existing = recipeClient.findByWebLink(FindByWebLinkParam(param.url))) {
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("url", existing.error.message))
            is Result.Success -> if (existing.value != null) {
                return Result.Failure(RecipeError.DuplicateWebLink(param.url))
            }
        }

        // Fetch HTML from URL
        val html = try {
            htmlFetcher.fetch(param.url)
        } catch (e: Exception) {
            logger.error("Failed to fetch URL: ${param.url}", e)
            return Result.Failure(RecipeError.ImportFailed(param.url, "Could not fetch the recipe page — check that the URL is valid and accessible"))
        }

        val catalogue = when (val result = drafter.loadCatalogue()) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }

        // Scrape recipe
        val scraped = when (val result = recipeScraperClient.scrape(ScrapeRecipeParam(
            html = html,
            sourceUrl = param.url,
            existingIngredients = catalogue.existing
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.ScrapeFailed(result.error.message))
        }

        val recipeId = when (val result = drafter.createDraft(scraped, webLink = param.url, userId = param.userId)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }
        return getRecipe.execute(GetRecipeParam(recipeId, param.userId))
    }
}
