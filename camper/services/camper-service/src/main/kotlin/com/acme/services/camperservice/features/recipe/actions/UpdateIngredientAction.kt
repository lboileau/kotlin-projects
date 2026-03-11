package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.api.UpdateIngredientParam as ClientUpdateIngredientParam
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.UpdateIngredientParam

internal class UpdateIngredientAction(
    private val ingredientClient: IngredientClient
) {
    fun execute(param: UpdateIngredientParam): Result<IngredientResponse, RecipeError> {
        return when (val result = ingredientClient.update(ClientUpdateIngredientParam(
            id = param.ingredientId,
            name = param.name,
            category = param.category,
            defaultUnit = param.defaultUnit
        ))) {
            is Result.Success -> Result.Success(RecipeMapper.toIngredientResponse(result.value))
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(RecipeError.IngredientNotFound(param.ingredientId))
                else -> Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
            }
        }
    }
}
