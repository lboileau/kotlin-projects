package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.ListIngredientsParam

internal class ListIngredientsAction(
    private val ingredientClient: IngredientClient
) {
    fun execute(param: ListIngredientsParam): Result<List<IngredientResponse>, RecipeError> {
        TODO("Not yet implemented")
    }
}
