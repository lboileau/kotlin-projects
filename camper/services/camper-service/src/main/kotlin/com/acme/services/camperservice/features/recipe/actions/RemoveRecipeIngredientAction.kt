package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.RemoveRecipeIngredientParam as ClientRemoveParam
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.RemoveRecipeIngredientParam

internal class RemoveRecipeIngredientAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: RemoveRecipeIngredientParam): Result<Unit, RecipeError> {
        // Validate recipe exists and user is creator
        val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        if (recipe.createdBy != param.userId) {
            return Result.Failure(RecipeError.NotCreator(param.recipeId, param.userId))
        }

        // Verify the recipe ingredient belongs to this recipe
        val ingredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        if (ingredients.none { it.id == param.recipeIngredientId }) {
            return Result.Failure(RecipeError.Invalid("ingredientId", "Recipe ingredient not found: ${param.recipeIngredientId}"))
        }

        return when (val result = recipeClient.removeIngredient(ClientRemoveParam(param.recipeIngredientId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
        }
    }
}
