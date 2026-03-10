package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.CreateIngredientParam

internal class CreateIngredientAction(
    private val ingredientClient: IngredientClient
) {
    fun execute(param: CreateIngredientParam): Result<IngredientResponse, RecipeError> {
        TODO("Not yet implemented")
    }
}
