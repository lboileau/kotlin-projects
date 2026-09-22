package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.GetRecipeStepsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import java.util.UUID

internal class GetRecipeAction(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
    private val photoStore: RecipePhotoStore
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

        // One batched read covers both this recipe and its nested duplicateOf. Ids absent from
        // the result have no favourites at all, so they default to 0 / false.
        val summaries = when (val result = recipeClient.getFavoriteSummaries(
            GetRecipeFavoriteSummariesParam(
                recipeIds = listOfNotNull(recipe.id, recipe.duplicateOfId),
                userId = param.userId
            )
        )) {
            is Result.Success -> result.value.associateBy { it.recipeId }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        val duplicateOf = recipe.duplicateOfId?.let { dupId ->
            when (val result = recipeClient.getById(RecipeGetByIdParam(dupId))) {
                is Result.Success -> RecipeMapper.toRecipeResponse(
                    result.value,
                    summaries[dupId]?.favoriteCount ?: 0,
                    summaries[dupId]?.favoritedByMe ?: false
                )
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

        val steps = when (val result = recipeClient.getSteps(GetRecipeStepsParam(recipe.id))) {
            is Result.Success -> result.value.map { it.text }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("steps", result.error.message))
        }

        // Each photo's URL comes from the store (a presigned URL in production, memoised there).
        val photos = when (val result = photoStore.photos(recipe.id)) {
            is Result.Success -> when (val responses = photoStore.toResponses(result.value)) {
                is Result.Success -> responses.value
                is Result.Failure -> return responses
            }
            is Result.Failure -> return result
        }

        return Result.Success(
            RecipeMapper.toRecipeDetailResponse(
                recipe,
                duplicateOf,
                ingredientResponses,
                summaries[recipe.id]?.favoriteCount ?: 0,
                summaries[recipe.id]?.favoritedByMe ?: false,
                steps,
                photos
            )
        )
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
