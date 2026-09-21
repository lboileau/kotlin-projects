package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.GetAllParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ListRecipesParam

internal class ListRecipesAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: ListRecipesParam): Result<List<RecipeResponse>, RecipeError> {
        val published = when (val result = recipeClient.getAll(GetAllParam(status = "published"))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipes", result.error.message))
        }

        val ownRecipes = when (val result = recipeClient.getAll(GetAllParam(createdBy = param.userId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipes", result.error.message))
        }

        val visible = (published + ownRecipes)
            .distinctBy { it.id }
            .sortedBy { it.name }

        // ONE batched favourite read for the whole page — never one per recipe. Total client
        // calls stays at 3 (two getAll plus this), independent of how many recipes come back.
        val summaries = if (visible.isEmpty()) {
            emptyMap()
        } else {
            when (val result = recipeClient.getFavoriteSummaries(
                GetRecipeFavoriteSummariesParam(recipeIds = visible.map { it.id }, userId = param.userId)
            )) {
                is Result.Success -> result.value.associateBy { it.recipeId }
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
            }
        }

        // Recipes nobody has favourited are absent from the summaries, so default them to 0 / false.
        return Result.Success(
            visible.map { recipe ->
                val summary = summaries[recipe.id]
                RecipeMapper.toRecipeResponse(
                    recipe,
                    summary?.favoriteCount ?: 0,
                    summary?.favoritedByMe ?: false
                )
            }
        )
    }
}
