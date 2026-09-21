package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.RemoveRecipeFavoriteParam
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.UnfavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.validations.ValidateUnfavoriteRecipe
import org.slf4j.LoggerFactory

/**
 * Un-favourite a recipe on behalf of the caller. Idempotent — removing a
 * favourite that was never there still succeeds.
 *
 * Identical in shape to [FavoriteRecipeAction], including the 404-never-403
 * visibility rule.
 */
internal class UnfavoriteRecipeAction(
    private val recipeClient: RecipeClient
) {
    private val logger = LoggerFactory.getLogger(UnfavoriteRecipeAction::class.java)
    private val validate = ValidateUnfavoriteRecipe()

    fun execute(param: UnfavoriteRecipeParam): Result<RecipeFavoriteStatusResponse, RecipeError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val recipe = when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }
        if (!RecipeVisibility.isVisibleTo(recipe, param.userId)) {
            return Result.Failure(RecipeError.NotFound(param.recipeId))
        }

        logger.debug("Un-favouriting recipe={} for user={}", param.recipeId, param.userId)
        when (val result = recipeClient.removeFavorite(RemoveRecipeFavoriteParam(param.recipeId, param.userId))) {
            is Result.Success -> {}
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorite", result.error.message))
        }

        val summaries = when (val result = recipeClient.getFavoriteSummaries(
            GetRecipeFavoriteSummariesParam(recipeIds = listOf(param.recipeId), userId = param.userId)
        )) {
            is Result.Success -> result.value.associateBy { it.recipeId }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        // Removing the LAST favourite is the normal path here: the recipe then has zero
        // favourites and getFavoriteSummaries omits it entirely, so the read comes back EMPTY.
        // The default below is what answers that case — never .single()/.first()/[0].
        val summary = summaries[param.recipeId] ?: RecipeFavoriteSummary(param.recipeId, 0, false)
        return Result.Success(RecipeMapper.toFavoriteStatusResponse(summary))
    }
}
