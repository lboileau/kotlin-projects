package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.ImportRecipeParam

internal class ImportRecipeAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
    private val recipeScraperClient: RecipeScraperClient
) {
    fun execute(param: ImportRecipeParam): Result<RecipeDetailResponse, RecipeError> {
        TODO("Not yet implemented")
    }
}
