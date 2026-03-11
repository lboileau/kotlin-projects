package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import java.util.UUID

internal class GetRecipeAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {
    fun execute(param: GetRecipeParam): Result<RecipeDetailResponse, RecipeError> {
        val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        // Build a map of all unique ingredient IDs for enrichment
        val ingredientIds = recipeIngredients
            .flatMap { listOfNotNull(it.ingredientId, it.matchedIngredientId) }
            .toSet()

        val ingredientMap = buildIngredientMap(ingredientIds)
            ?: return Result.Failure(RecipeError.Invalid("ingredients", "Failed to load ingredient details"))

        val duplicateOf = recipe.duplicateOfId?.let { dupId ->
            when (val result = recipeClient.getById(RecipeGetByIdParam(dupId))) {
                is Result.Success -> RecipeMapper.toRecipeResponse(result.value)
                is Result.Failure -> null
            }
        }

        val ingredientResponses = recipeIngredients.map { ri ->
            RecipeMapper.toRecipeIngredientResponse(
                recipeIngredient = ri,
                ingredient = ri.ingredientId?.let { ingredientMap[it] },
                matchedIngredient = ri.matchedIngredientId?.let { ingredientMap[it] }
            )
        }

        return Result.Success(RecipeMapper.toRecipeDetailResponse(recipe, duplicateOf, ingredientResponses))
    }

    private fun buildIngredientMap(ids: Set<UUID>): Map<UUID, com.acme.services.camperservice.features.recipe.dto.IngredientResponse>? {
        val map = mutableMapOf<UUID, com.acme.services.camperservice.features.recipe.dto.IngredientResponse>()
        for (id in ids) {
            when (val result = ingredientClient.getById(IngredientGetByIdParam(id))) {
                is Result.Success -> map[id] = RecipeMapper.toIngredientResponse(result.value)
                is Result.Failure -> return null
            }
        }
        return map
    }
}
