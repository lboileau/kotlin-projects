package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.ImportRecipeFromImagesParam
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * Same import as [ImportRecipeAction] with photos in place of a page: nothing to fetch and no web
 * link to dedupe on, so the draft is created with `webLink = null`.
 */
internal class ImportRecipeFromImagesAction(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    private val recipeScraperClient: RecipeScraperClient
) {
    private val logger = LoggerFactory.getLogger(ImportRecipeFromImagesAction::class.java)
    private val drafter = ScrapedRecipeDrafter(recipeClient, ingredientClient)

    fun execute(param: ImportRecipeFromImagesParam): Result<RecipeDetailResponse, RecipeError> {
        val images = when (val validated = validateImages(param)) {
            is Result.Success -> validated.value
            is Result.Failure -> return validated
        }

        val catalogue = when (val result = drafter.loadCatalogue()) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }

        logger.info("Importing recipe from {} photo(s)", images.size)
        val scraped = when (val result = recipeScraperClient.scrapeImages(ScrapeRecipeImagesParam(
            images = images,
            existingIngredients = catalogue.existing
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.ScrapeFailed(result.error.message))
        }

        return drafter.createDraft(scraped, webLink = null, userId = param.userId, catalogue = catalogue)
    }

    /**
     * The caller has already sized the photos down; this is the last line before the API's own
     * limits (5 MB per image) so a bad request fails here with a reason instead of there.
     */
    private fun validateImages(param: ImportRecipeFromImagesParam): Result<List<RecipeImage>, RecipeError> {
        if (param.images.isEmpty()) {
            return Result.Failure(RecipeError.Invalid("images", "at least one photo is required"))
        }
        if (param.images.size > RecipeImage.MAX_IMAGES) {
            return Result.Failure(RecipeError.Invalid("images", "at most ${RecipeImage.MAX_IMAGES} photos"))
        }
        val images = param.images.mapIndexed { i, image ->
            val mediaType = image.mediaType.trim().lowercase()
            if (mediaType !in RecipeImage.SUPPORTED_MEDIA_TYPES) {
                return Result.Failure(RecipeError.Invalid("images[$i].mediaType", "must be one of ${RecipeImage.SUPPORTED_MEDIA_TYPES.sorted().joinToString()}"))
            }
            val data = image.data.trim()
            if (data.isEmpty()) {
                return Result.Failure(RecipeError.Invalid("images[$i].data", "must not be blank"))
            }
            if (data.startsWith("data:")) {
                return Result.Failure(RecipeError.Invalid("images[$i].data", "must be raw base64, not a data: URL"))
            }
            // Base64 is 4 chars per 3 bytes; check before decoding so an oversized upload doesn't get decoded at all.
            if (data.length > MAX_IMAGE_BYTES / 3 * 4 + 4) {
                return Result.Failure(RecipeError.Invalid("images[$i].data", "photo is larger than ${MAX_IMAGE_BYTES / 1024 / 1024} MB"))
            }
            val decodedSize = try {
                Base64.getDecoder().decode(data).size
            } catch (e: IllegalArgumentException) {
                return Result.Failure(RecipeError.Invalid("images[$i].data", "is not valid base64"))
            }
            if (decodedSize > MAX_IMAGE_BYTES) {
                return Result.Failure(RecipeError.Invalid("images[$i].data", "photo is larger than ${MAX_IMAGE_BYTES / 1024 / 1024} MB"))
            }
            RecipeImage(mediaType = mediaType, base64Data = data)
        }
        return Result.Success(images)
    }

    companion object {
        /** The Claude API's per-image limit. */
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    }
}
