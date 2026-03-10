package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.CreateRecipeParam

internal class CreateRecipeAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {
    fun execute(param: CreateRecipeParam): Result<RecipeResponse, RecipeError> {
        TODO("Not yet implemented")
    }
}
