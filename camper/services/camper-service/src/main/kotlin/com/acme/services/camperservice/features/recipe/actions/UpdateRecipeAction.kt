package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.UpdateRecipeParam

internal class UpdateRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: UpdateRecipeParam): Result<RecipeResponse, RecipeError> {
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        // For description, meal, and theme: null/absent means "unchanged", a present-but-blank
        // string means "clear this field" (NULL it out), and a non-blank string updates it.
        val updated = when (val result = recipeClient.update(ClientUpdateRecipeParam(
            id = param.recipeId,
            name = param.name,
            description = param.description?.takeIf { it.isNotBlank() },
            clearDescription = param.description != null && param.description.isBlank(),
            baseServings = param.baseServings,
            meal = param.meal?.takeIf { it.isNotBlank() },
            clearMeal = param.meal != null && param.meal.isBlank(),
            theme = param.theme?.takeIf { it.isNotBlank() },
            clearTheme = param.theme != null && param.theme.isBlank(),
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }

        // An edit never changes who favourited the recipe, but the response must still carry the
        // real numbers — returning 0 / false here would poison the frontend's cache.
        val summaries = when (val result = recipeClient.getFavoriteSummaries(
            GetRecipeFavoriteSummariesParam(recipeIds = listOf(param.recipeId), userId = param.userId)
        )) {
            is Result.Success -> result.value.associateBy { it.recipeId }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        val summary = summaries[param.recipeId]
        return Result.Success(
            RecipeMapper.toRecipeResponse(updated, summary?.favoriteCount ?: 0, summary?.favoritedByMe ?: false)
        )
    }
}
