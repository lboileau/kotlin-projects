package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.FindByNameParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.api.CreateIngredientParam as ClientCreateIngredientParam
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.CreateIngredientParam

internal class CreateIngredientAction(
    private val ingredientClient: IngredientClient
) {
    fun execute(param: CreateIngredientParam): Result<IngredientResponse, RecipeError> {
        if (param.name.isBlank()) {
            return Result.Failure(RecipeError.Invalid("name", "must not be blank"))
        }

        when (val existing = ingredientClient.findByName(FindByNameParam(param.name))) {
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("name", existing.error.message))
            is Result.Success -> if (existing.value != null) {
                return Result.Failure(RecipeError.DuplicateIngredientName(param.name))
            }
        }

        return when (val result = ingredientClient.create(ClientCreateIngredientParam(
            name = param.name,
            category = param.category,
            defaultUnit = param.defaultUnit
        ))) {
            is Result.Success -> Result.Success(RecipeMapper.toIngredientResponse(result.value))
            is Result.Failure -> Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
        }
    }
}
