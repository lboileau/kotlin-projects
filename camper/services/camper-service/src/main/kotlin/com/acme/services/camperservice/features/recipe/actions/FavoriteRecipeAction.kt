package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.FavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.validations.ValidateFavoriteRecipe

/**
 * Favourite a recipe on behalf of the caller. Idempotent.
 *
 * Contract only — the body lands in the service-implementation PR. It will:
 * validate, fetch the recipe and apply [RecipeVisibility] (hidden or missing →
 * [RecipeError.NotFound], never 403), call `recipeClient.addFavorite`, then read
 * the recipe's favourite summary back, defaulting a missing row to `0 / false`.
 */
internal class FavoriteRecipeAction(
    private val recipeClient: RecipeClient
) {
    private val validate = ValidateFavoriteRecipe()

    fun execute(param: FavoriteRecipeParam): Result<RecipeFavoriteStatusResponse, RecipeError> {
        TODO("Implementation in service-impl PR")
    }
}
