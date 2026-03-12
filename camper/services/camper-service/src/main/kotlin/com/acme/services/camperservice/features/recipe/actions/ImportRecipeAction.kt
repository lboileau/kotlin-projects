package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import com.acme.clients.recipeclient.api.FindByWebLinkParam
import com.acme.clients.recipeclient.api.FindSimilarParam
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.clients.recipeclient.api.CreateRecipeParam as ClientCreateRecipeParam
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
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
    private val ingredientClient: IngredientClient,
    private val recipeScraperClient: RecipeScraperClient,
    private val htmlFetcher: HtmlFetcher = defaultHtmlFetcher()
) {
    private val logger = LoggerFactory.getLogger(ImportRecipeAction::class.java)

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

        // Load all global ingredients
        val allIngredients = when (val result = ingredientClient.getAll()) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        val existingIngredients = allIngredients.map { ing ->
            ExistingIngredient(id = ing.id, name = ing.name, category = ing.category, defaultUnit = ing.defaultUnit)
        }

        // Scrape recipe
        val scraped = when (val result = recipeScraperClient.scrape(ScrapeRecipeParam(
            html = html,
            sourceUrl = param.url,
            existingIngredients = existingIngredients
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.ScrapeFailed(result.error.message))
        }

        // Check for similar existing recipes (duplicate detection)
        val similarRecipes = when (val result = recipeClient.findSimilarByName(FindSimilarParam(scraped.name))) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }
        val duplicateOfId = similarRecipes.firstOrNull()?.id

        // Create draft recipe
        val recipe = when (val result = recipeClient.create(ClientCreateRecipeParam(
            name = scraped.name,
            description = scraped.description,
            webLink = param.url,
            baseServings = scraped.baseServings,
            status = "draft",
            createdBy = param.userId,
            meal = scraped.meal,
            theme = scraped.theme
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }

        // Set duplicate_of_id if found
        val finalRecipe = if (duplicateOfId != null) {
            when (val result = recipeClient.update(ClientUpdateRecipeParam(
                id = recipe.id,
                duplicateOfId = duplicateOfId
            ))) {
                is Result.Success -> result.value
                is Result.Failure -> recipe
            }
        } else {
            recipe
        }

        // Add recipe ingredients with review state
        val ingredientParams = scraped.ingredients.map { ing ->
            AddRecipeIngredientParam(
                recipeId = recipe.id,
                ingredientId = if (ing.reviewFlags.isEmpty() && ing.matchedIngredientId != null) ing.matchedIngredientId else null,
                originalText = ing.originalText,
                quantity = ing.quantity,
                unit = ing.unit,
                status = if (ing.reviewFlags.isEmpty() && ing.matchedIngredientId != null) "approved" else "pending_review",
                matchedIngredientId = ing.matchedIngredientId,
                suggestedIngredientName = ing.suggestedIngredientName,
                suggestedCategory = ing.suggestedCategory,
                suggestedUnit = ing.suggestedUnit,
                reviewFlags = ing.reviewFlags
            )
        }

        if (ingredientParams.isNotEmpty()) {
            when (val result = recipeClient.addIngredients(AddRecipeIngredientsParam(ingredientParams))) {
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
                is Result.Success -> {}
            }
        }

        // Fetch full detail for response
        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        val ingredientMap = allIngredients.associateBy({ it.id }, { RecipeMapper.toIngredientResponse(it) })

        val duplicateOf = finalRecipe.duplicateOfId?.let { dupId ->
            when (val result = recipeClient.getById(GetByIdParam(dupId))) {
                is Result.Success -> RecipeMapper.toRecipeResponse(result.value)
                is Result.Failure -> null
            }
        }

        val ingredientResponses = recipeIngredients.map { ri ->
            RecipeMapper.toRecipeIngredientResponse(
                recipeIngredient = ri,
                ingredient = ri.ingredientId?.let { ingredientMap[it] },
                matchedIngredient = ri.matchedIngredientId?.let { ingredientMap[it] }
            )
        }

        return Result.Success(RecipeMapper.toRecipeDetailResponse(finalRecipe, duplicateOf, ingredientResponses))
    }
}
