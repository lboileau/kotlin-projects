package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteUserResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.ListRecipeFavoritesParam
import com.acme.services.camperservice.features.recipe.validations.ValidateListRecipeFavorites

/**
 * Who favourited a recipe, oldest first.
 *
 * Contract only — the body lands in the service-implementation PR. It will:
 * validate, fetch the recipe and apply [RecipeVisibility], call
 * `recipeClient.getFavorites`, then resolve each display name with one
 * `userClient.getById` per row (`username ?: email`, falling back to the id
 * string on lookup failure) — the same enrichment plan members use.
 */
internal class ListRecipeFavoritesAction(
    private val recipeClient: RecipeClient,
    private val userClient: UserClient
) {
    private val validate = ValidateListRecipeFavorites()

    fun execute(param: ListRecipeFavoritesParam): Result<List<RecipeFavoriteUserResponse>, RecipeError> {
        TODO("Implementation in service-impl PR")
    }
}
