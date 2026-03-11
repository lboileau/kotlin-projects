package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.DeleteRecipeParam
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ResolveDuplicateParam

internal class ResolveDuplicateAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: ResolveDuplicateParam): Result<RecipeResponse?, RecipeError> {
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        return when (param.action) {
            "NOT_DUPLICATE" -> {
                when (val result = recipeClient.update(ClientUpdateRecipeParam(
                    id = param.recipeId,
                    clearDuplicateOf = true
                ))) {
                    is Result.Success -> Result.Success(RecipeMapper.toRecipeResponse(result.value))
                    is Result.Failure -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
                }
            }
            "USE_EXISTING" -> {
                when (val result = recipeClient.delete(DeleteRecipeParam(param.recipeId))) {
                    is Result.Success -> Result.Success(null)
                    is Result.Failure -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
                }
            }
            else -> Result.Failure(RecipeError.Invalid("action", "must be NOT_DUPLICATE or USE_EXISTING"))
        }
    }
}
