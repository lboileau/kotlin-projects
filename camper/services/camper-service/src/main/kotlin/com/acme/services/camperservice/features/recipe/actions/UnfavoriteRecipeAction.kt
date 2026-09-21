package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.UnfavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.validations.ValidateUnfavoriteRecipe

/**
 * Un-favourite a recipe on behalf of the caller. Idempotent — removing a
 * favourite that was never there still succeeds.
 *
 * Contract only — the body lands in the service-implementation PR. Identical in
 * shape to [FavoriteRecipeAction], with `recipeClient.removeFavorite`.
 */
internal class UnfavoriteRecipeAction(
    private val recipeClient: RecipeClient
) {
    private val validate = ValidateUnfavoriteRecipe()

    fun execute(param: UnfavoriteRecipeParam): Result<RecipeFavoriteStatusResponse, RecipeError> {
        TODO("Implementation in service-impl PR")
    }
}
