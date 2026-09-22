package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam as ClientReplaceRecipeStepsParam
import com.acme.services.camperservice.features.recipe.dto.RecipeStepsResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.ReplaceRecipeStepsParam

internal class ReplaceRecipeStepsAction(
    private val recipeClient: RecipeClient
) {
    fun execute(param: ReplaceRecipeStepsParam): Result<RecipeStepsResponse, RecipeError> {
        val steps = when (val validated = validateSteps(param.steps)) {
            is Result.Success -> validated.value
            is Result.Failure -> return validated
        }
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> {}
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }
        return when (val result = recipeClient.replaceSteps(ClientReplaceRecipeStepsParam(param.recipeId, steps))) {
            is Result.Success -> Result.Success(RecipeStepsResponse(result.value.map { it.text }))
            is Result.Failure -> Result.Failure(RecipeError.Invalid("steps", result.error.message))
        }
    }

    companion object {
        const val MAX_STEPS = 100
        const val MAX_STEP_CHARS = 2000

        /** Trimmed, non-blank, bounded. Shared with create so both writes agree on what a step is. */
        fun validateSteps(steps: List<String>): Result<List<String>, RecipeError> {
            if (steps.size > MAX_STEPS) return Result.Failure(RecipeError.Invalid("steps", "at most $MAX_STEPS steps"))
            val trimmed = steps.map { it.trim() }
            trimmed.forEachIndexed { i, text ->
                if (text.isEmpty()) return Result.Failure(RecipeError.Invalid("steps[$i]", "must not be blank"))
                if (text.length > MAX_STEP_CHARS) return Result.Failure(RecipeError.Invalid("steps[$i]", "at most $MAX_STEP_CHARS characters"))
            }
            return Result.Success(trimmed)
        }
    }
}
