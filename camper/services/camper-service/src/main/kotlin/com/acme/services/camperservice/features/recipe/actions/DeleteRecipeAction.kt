package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.DeleteRecipeParam

internal class DeleteRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: DeleteRecipeParam): Result<Unit, RecipeError> {
        TODO("Not yet implemented")
    }
}
