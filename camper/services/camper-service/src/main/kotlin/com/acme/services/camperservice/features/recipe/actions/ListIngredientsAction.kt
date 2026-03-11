package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ListIngredientsParam

internal class ListIngredientsAction(
    private val ingredientClient: IngredientClient
) {
    fun execute(param: ListIngredientsParam): Result<List<IngredientResponse>, RecipeError> {
        return when (val result = ingredientClient.getAll()) {
            is Result.Success -> Result.Success(result.value.map { RecipeMapper.toIngredientResponse(it) })
            is Result.Failure -> Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }
    }
}
