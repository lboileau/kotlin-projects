package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.AddRecipeFavoriteParam
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.FavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.validations.ValidateFavoriteRecipe
import org.slf4j.LoggerFactory

/**
 * Favourite a recipe on behalf of the caller. Idempotent — favouriting an
 * already favourited recipe succeeds and changes nothing.
 *
 * A recipe the caller cannot see (someone else's draft) and a recipe that does
 * not exist are both [RecipeError.NotFound] → 404, never 403, so the existence
 * of another person's draft is never disclosed.
 */
internal class FavoriteRecipeAction(
    private val recipeClient: RecipeClient
) {
    private val logger = LoggerFactory.getLogger(FavoriteRecipeAction::class.java)
    private val validate = ValidateFavoriteRecipe()

    fun execute(param: FavoriteRecipeParam): Result<RecipeFavoriteStatusResponse, RecipeError> {
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

        logger.debug("Favouriting recipe={} for user={}", param.recipeId, param.userId)
        when (val result = recipeClient.addFavorite(AddRecipeFavoriteParam(param.recipeId, param.userId))) {
            is Result.Success -> {}
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorite", result.error.message))
        }

        val summaries = when (val result = recipeClient.getFavoriteSummaries(
            GetRecipeFavoriteSummariesParam(recipeIds = listOf(param.recipeId), userId = param.userId)
        )) {
            is Result.Success -> result.value.associateBy { it.recipeId }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        // getFavoriteSummaries OMITS recipes with zero favourites, so a missing row is a normal
        // answer, not an anomaly — default it rather than indexing into the list.
        val summary = summaries[param.recipeId] ?: RecipeFavoriteSummary(param.recipeId, 0, false)
        return Result.Success(RecipeMapper.toFavoriteStatusResponse(summary))
    }
}
