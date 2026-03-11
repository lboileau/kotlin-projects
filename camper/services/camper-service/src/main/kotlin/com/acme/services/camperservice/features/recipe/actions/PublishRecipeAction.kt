package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.PublishRecipeParam

internal class PublishRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: PublishRecipeParam): Result<RecipeResponse, RecipeError> {
        val recipe = when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        if (recipe.status == "published") {
            return Result.Failure(RecipeError.AlreadyPublished(param.recipeId))
        }

        // Check for unresolved duplicate
        if (recipe.duplicateOfId != null) {
            return Result.Failure(RecipeError.UnresolvedDuplicate(param.recipeId))
        }

        // Check all ingredients are approved
        val ingredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        val pendingCount = ingredients.count { it.status == "pending_review" }
        if (pendingCount > 0) {
            return Result.Failure(RecipeError.UnresolvedIngredients(param.recipeId, pendingCount))
        }

        return when (val result = recipeClient.update(ClientUpdateRecipeParam(
            id = param.recipeId,
            status = "published"
        ))) {
            is Result.Success -> Result.Success(RecipeMapper.toRecipeResponse(result.value))
            is Result.Failure -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }
    }
}
