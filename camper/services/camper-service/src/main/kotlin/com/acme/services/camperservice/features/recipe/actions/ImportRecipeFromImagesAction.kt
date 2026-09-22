package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import com.acme.services.camperservice.features.recipe.params.ImportRecipeFromImagesParam
import org.slf4j.LoggerFactory

/**
 * Same import as [ImportRecipeAction] with photos in place of a page: nothing to fetch and no web
 * link to dedupe on, so the draft is created with `webLink = null`. The photos the recipe was
 * read from are attached to the draft afterwards, as its first photos.
 */
internal class ImportRecipeFromImagesAction(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    private val recipeScraperClient: RecipeScraperClient,
    private val photoStore: RecipePhotoStore
) {
    private val logger = LoggerFactory.getLogger(ImportRecipeFromImagesAction::class.java)
    private val drafter = ScrapedRecipeDrafter(recipeClient, ingredientClient)
    private val getRecipe = GetRecipeAction(recipeClient, ingredientClient, photoStore)

    private class RoleImage(val role: String?, val image: DecodedImage)

    fun execute(param: ImportRecipeFromImagesParam): Result<RecipeDetailResponse, RecipeError> {
        val images = when (val validated = validateImages(param)) {
            is Result.Success -> validated.value
            is Result.Failure -> return validated
        }

        val catalogue = when (val result = drafter.loadCatalogue()) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }

        logger.info("Importing recipe from {} photo(s) [{}]", images.size, images.joinToString { it.role ?: "unlabelled" })
        val scraped = when (val result = recipeScraperClient.scrapeImages(ScrapeRecipeImagesParam(
            images = images.map { RecipeImage(mediaType = it.image.mediaType, base64Data = it.image.base64, role = it.role) },
            existingIngredients = catalogue.existing
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.ScrapeFailed(result.error.message))
        }

        val recipeId = when (val result = drafter.createDraft(scraped, webLink = null, userId = param.userId)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }

        // The source photos become the draft's photos. Losing one to a storage hiccup is not
        // worth losing the draft over: it is logged, and the user can add the photo by hand.
        images.forEach { entry ->
            when (val stored = photoStore.store(recipeId, param.userId, entry.image, RecipePhoto.SOURCE_IMPORT, entry.role)) {
                is Result.Failure -> logger.error("Could not attach the {} photo to recipe {}: {}", entry.role ?: "source", recipeId, stored.error.message)
                is Result.Success -> {}
            }
        }

        return getRecipe.execute(GetRecipeParam(recipeId, param.userId))
    }

    /**
     * One or two photos, each decoded and size-checked ([RecipePhotoStore.decode]); with roles,
     * at most one per role and the ingredients one required. A single photo may carry no role.
     */
    private fun validateImages(param: ImportRecipeFromImagesParam): Result<List<RoleImage>, RecipeError> {
        if (param.images.isEmpty()) {
            return Result.Failure(RecipeError.Invalid("images", "at least one photo is required"))
        }
        if (param.images.size > RecipeImage.MAX_IMAGES) {
            return Result.Failure(RecipeError.Invalid("images", "at most ${RecipeImage.MAX_IMAGES} photos: one of the ingredients, one of the instructions"))
        }
        val roles = param.images.map { it.role?.trim()?.lowercase()?.ifEmpty { null } }
        roles.forEachIndexed { i, role ->
            if (role != null && role !in RecipeImage.ROLES) {
                return Result.Failure(RecipeError.Invalid("images[$i].role", "must be one of ${RecipeImage.ROLES.sorted().joinToString()}"))
            }
        }
        if (roles.count { it == RecipeImage.ROLE_INGREDIENTS } > 1 || roles.count { it == RecipeImage.ROLE_INSTRUCTIONS } > 1) {
            return Result.Failure(RecipeError.Invalid("images", "at most one photo per role"))
        }
        if (param.images.size > 1 && roles.any { it == null }) {
            return Result.Failure(RecipeError.Invalid("images", "with two photos each needs a role"))
        }
        if (roles.any { it != null } && RecipeImage.ROLE_INGREDIENTS !in roles) {
            return Result.Failure(RecipeError.Invalid("images", "the ingredients photo is required"))
        }
        val decoded = param.images.mapIndexed { i, image ->
            when (val result = photoStore.decode("images[$i]", image.mediaType, image.data)) {
                is Result.Success -> RoleImage(roles[i], result.value)
                is Result.Failure -> return result
            }
        }
        return Result.Success(decoded)
    }
}
