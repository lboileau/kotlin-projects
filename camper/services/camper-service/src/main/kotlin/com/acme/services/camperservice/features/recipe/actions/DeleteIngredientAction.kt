package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.DeleteIngredientParam as ClientDeleteParam
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.FindRecipeIngredientsByIngredientIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeIngredientParam
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.DeleteIngredientParam
import java.util.UUID

internal class DeleteIngredientAction(
    private val ingredientClient: IngredientClient,
    private val recipeClient: RecipeClient
) {
    fun execute(param: DeleteIngredientParam): Result<Unit, RecipeError> {
        // Verify ingredient exists
        when (val result = ingredientClient.getById(IngredientGetByIdParam(param.ingredientId))) {
            is Result.Success -> {} // ok
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.IngredientNotFound(param.ingredientId))
                else -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
            }
        }

        // Find all recipe_ingredients that reference this ingredient
        val affectedRecipeIngredients = when (val result = recipeClient.findIngredientsByIngredientId(
            FindRecipeIngredientsByIngredientIdParam(param.ingredientId)
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
        }

        // Collect affected recipe IDs for drafting
        val affectedRecipeIds = mutableSetOf<UUID>()

        // Null out ingredient_id on affected recipe_ingredients and set to pending_review
        for (ri in affectedRecipeIngredients) {
            affectedRecipeIds.add(ri.recipeId)
            when (val result = recipeClient.updateIngredient(UpdateRecipeIngredientParam(
                id = ri.id,
                ingredientId = null,
                status = "pending_review",
                reviewFlags = listOf("ingredient_deleted"),
                matchedIngredientId = null,
                clearMatchedIngredient = true
            ))) {
                is Result.Success -> {} // ok
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
            }
        }

        // Set affected recipes back to draft
        for (recipeId in affectedRecipeIds) {
            when (val result = recipeClient.update(ClientUpdateRecipeParam(
                id = recipeId,
                status = "draft"
            ))) {
                is Result.Success -> {} // ok
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        // Now delete the ingredient (FK is SET NULL so DB won't block)
        return when (val result = ingredientClient.delete(ClientDeleteParam(param.ingredientId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(RecipeError.IngredientNotFound(param.ingredientId))
                else -> Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
            }
        }
    }
}
