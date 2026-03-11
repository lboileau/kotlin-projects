package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.DeleteRecipeParam as ClientDeleteRecipeParam
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.DeleteRecipeParam

internal class DeleteRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: DeleteRecipeParam): Result<Unit, RecipeError> {
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        return when (val result = recipeClient.delete(ClientDeleteRecipeParam(param.recipeId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }
    }
}
