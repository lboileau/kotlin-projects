package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.UpdateRecipeParam

internal class UpdateRecipeAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: UpdateRecipeParam): Result<RecipeResponse, RecipeError> {
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }

        // For description, meal, and theme: null/absent means "unchanged", a present-but-blank
        // string means "clear this field" (NULL it out), and a non-blank string updates it.
        return when (val result = recipeClient.update(ClientUpdateRecipeParam(
            id = param.recipeId,
            name = param.name,
            description = param.description?.takeIf { it.isNotBlank() },
            clearDescription = param.description != null && param.description.isBlank(),
            baseServings = param.baseServings,
            meal = param.meal?.takeIf { it.isNotBlank() },
            clearMeal = param.meal != null && param.meal.isBlank(),
            theme = param.theme?.takeIf { it.isNotBlank() },
            clearTheme = param.theme != null && param.theme.isBlank(),
        ))) {
            is Result.Success -> Result.Success(RecipeMapper.toRecipeResponse(result.value))
            is Result.Failure -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }
    }
}
