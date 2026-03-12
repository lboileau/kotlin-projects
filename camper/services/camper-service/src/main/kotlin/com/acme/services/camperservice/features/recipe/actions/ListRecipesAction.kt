package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.GetAllParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ListRecipesParam

internal class ListRecipesAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: ListRecipesParam): Result<List<RecipeResponse>, RecipeError> {
        val all = when (val result = recipeClient.getAll(GetAllParam())) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipes", result.error.message))
        }

        return Result.Success(all.map { RecipeMapper.toRecipeResponse(it) })
    }
}
