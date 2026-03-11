package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.FindByNameParam
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.api.CreateIngredientParam as ClientCreateIngredientParam
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeIngredientParam
import com.acme.services.camperservice.features.recipe.dto.RecipeIngredientResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ResolveIngredientParam

internal class ResolveIngredientAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {
    fun execute(param: ResolveIngredientParam): Result<RecipeIngredientResponse, RecipeError> {
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

        // Find the specific recipe ingredient
        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        val recipeIngredient = recipeIngredients.find { it.id == param.recipeIngredientId }
            ?: return Result.Failure(RecipeError.Invalid("ingredientId", "Recipe ingredient not found: ${param.recipeIngredientId}"))

        val resolvedIngredientId = when (param.action) {
            "CONFIRM_MATCH" -> {
                val matchedId = recipeIngredient.matchedIngredientId
                    ?: return Result.Failure(RecipeError.Invalid("action", "No matched ingredient to confirm"))
                matchedId
            }
            "CREATE_NEW" -> {
                val newIngredient = param.newIngredient
                    ?: return Result.Failure(RecipeError.Invalid("newIngredient", "must be provided for CREATE_NEW"))

                // Check for duplicates first
                when (val existing = ingredientClient.findByName(FindByNameParam(newIngredient.name))) {
                    is Result.Failure -> return Result.Failure(RecipeError.Invalid("name", existing.error.message))
                    is Result.Success -> if (existing.value != null) {
                        return Result.Failure(RecipeError.DuplicateIngredientName(newIngredient.name))
                    }
                }

                when (val result = ingredientClient.create(ClientCreateIngredientParam(
                    name = newIngredient.name,
                    category = newIngredient.category,
                    defaultUnit = newIngredient.defaultUnit
                ))) {
                    is Result.Success -> result.value.id
                    is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
                }
            }
            "SELECT_EXISTING" -> {
                val ingredientId = param.ingredientId
                    ?: return Result.Failure(RecipeError.Invalid("ingredientId", "must be provided for SELECT_EXISTING"))

                // Validate ingredient exists
                when (val result = ingredientClient.getById(IngredientGetByIdParam(ingredientId))) {
                    is Result.Failure -> return Result.Failure(RecipeError.IngredientNotFound(ingredientId))
                    is Result.Success -> {}
                }

                ingredientId
            }
            else -> return Result.Failure(RecipeError.Invalid("action", "must be CONFIRM_MATCH, CREATE_NEW, or SELECT_EXISTING"))
        }

        val updatedRecipeIngredient = when (val result = recipeClient.updateIngredient(UpdateRecipeIngredientParam(
            id = recipeIngredient.id,
            ingredientId = resolvedIngredientId,
            quantity = param.quantity,
            unit = param.unit,
            status = "approved",
            reviewFlags = emptyList(),
            matchedIngredientId = null,
            clearMatchedIngredient = true
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredient", result.error.message))
        }

        val ingredient = when (val result = ingredientClient.getById(IngredientGetByIdParam(resolvedIngredientId))) {
            is Result.Success -> RecipeMapper.toIngredientResponse(result.value)
            is Result.Failure -> null
        }

        return Result.Success(RecipeMapper.toRecipeIngredientResponse(
            recipeIngredient = updatedRecipeIngredient,
            ingredient = ingredient,
            matchedIngredient = null
        ))
    }
}
