package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.PublishRecipeParam

internal class PublishRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: PublishRecipeParam): Result<RecipeResponse, RecipeError> {
        TODO("Not yet implemented")
    }
}
