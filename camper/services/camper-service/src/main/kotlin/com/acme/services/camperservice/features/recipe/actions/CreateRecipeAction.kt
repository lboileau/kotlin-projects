package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam
import com.acme.clients.recipeclient.api.CreateRecipeParam as ClientCreateRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.CreateRecipeParam

internal class CreateRecipeAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {
    fun execute(param: CreateRecipeParam): Result<RecipeResponse, RecipeError> {
        if (param.name.isBlank()) {
            return Result.Failure(RecipeError.Invalid("name", "must not be blank"))
        }
        if (param.baseServings <= 0) {
            return Result.Failure(RecipeError.Invalid("baseServings", "must be greater than 0"))
        }
        val steps = when (val validated = ReplaceRecipeStepsAction.validateSteps(param.steps)) {
            is Result.Success -> validated.value
            is Result.Failure -> return validated
        }

        // Validate all ingredient IDs exist
        for (ingredient in param.ingredients) {
            when (val result = ingredientClient.getById(IngredientGetByIdParam(ingredient.ingredientId))) {
                is Result.Failure -> return Result.Failure(RecipeError.IngredientNotFound(ingredient.ingredientId))
                is Result.Success -> {}
            }
        }

        val recipe = when (val result = recipeClient.create(ClientCreateRecipeParam(
            name = param.name,
            description = param.description,
            webLink = param.webLink,
            baseServings = param.baseServings,
            status = "published",
            createdBy = param.userId,
            meal = param.meal,
            theme = param.theme
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }

        if (param.ingredients.isNotEmpty()) {
            val ingredientParams = param.ingredients.map { ing ->
                AddRecipeIngredientParam(
                    recipeId = recipe.id,
                    ingredientId = ing.ingredientId,
                    originalText = null,
                    quantity = ing.quantity,
                    unit = ing.unit,
                    status = "approved",
                    matchedIngredientId = null,
                    suggestedIngredientName = null,
                    reviewFlags = emptyList()
                )
            }
            when (val result = recipeClient.addIngredients(AddRecipeIngredientsParam(ingredientParams))) {
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
                is Result.Success -> {}
            }
        }

        if (steps.isNotEmpty()) {
            when (val result = recipeClient.replaceSteps(ReplaceRecipeStepsParam(recipe.id, steps))) {
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("steps", result.error.message))
                is Result.Success -> {}
            }
        }

        // A recipe created microseconds ago cannot have favourites, so 0 / false is the real
        // answer here — this is the one call site that never reads a favourite summary.
        return Result.Success(RecipeMapper.toRecipeResponse(recipe, 0, false))
    }
}
