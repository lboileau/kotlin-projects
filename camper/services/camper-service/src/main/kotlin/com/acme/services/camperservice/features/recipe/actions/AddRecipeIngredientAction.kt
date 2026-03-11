package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam as ClientAddParam
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeIngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.AddRecipeIngredientParam

internal class AddRecipeIngredientAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {
    fun execute(param: AddRecipeIngredientParam): Result<RecipeIngredientResponse, RecipeError> {
        // Validate recipe exists and user is creator
        val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        if (recipe.createdBy != param.userId) {
            return Result.Failure(RecipeError.NotCreator(param.recipeId, param.userId))
        }

        // Validate ingredient exists
        val ingredient = when (val result = ingredientClient.getById(IngredientGetByIdParam(param.ingredientId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.IngredientNotFound(param.ingredientId))
                else -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
            }
        }

        // Add ingredient to recipe
        val recipeIngredient = when (val result = recipeClient.addIngredient(ClientAddParam(
            recipeId = param.recipeId,
            ingredientId = param.ingredientId,
            originalText = null,
            quantity = param.quantity,
            unit = param.unit,
            status = "approved",
            matchedIngredientId = null,
            suggestedIngredientName = null,
            reviewFlags = emptyList()
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
        }

        val ingredientResponse = com.acme.services.camperservice.features.recipe.dto.IngredientResponse(
            id = ingredient.id,
            name = ingredient.name,
            category = ingredient.category,
            defaultUnit = ingredient.defaultUnit,
            createdAt = ingredient.createdAt,
            updatedAt = ingredient.updatedAt
        )

        return Result.Success(RecipeIngredientResponse(
            id = recipeIngredient.id,
            recipeId = recipeIngredient.recipeId,
            ingredient = ingredientResponse,
            originalText = recipeIngredient.originalText,
            quantity = recipeIngredient.quantity,
            unit = recipeIngredient.unit,
            status = recipeIngredient.status,
            matchedIngredient = null,
            suggestedIngredientName = null,
            reviewFlags = recipeIngredient.reviewFlags,
            createdAt = recipeIngredient.createdAt,
            updatedAt = recipeIngredient.updatedAt
        ))
    }
}
