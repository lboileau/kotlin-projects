package com.acme.services.camperservice.features.recipe.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.UnfavoriteRecipeParam
import org.slf4j.LoggerFactory

internal class ValidateUnfavoriteRecipe {
    private val logger = LoggerFactory.getLogger(ValidateUnfavoriteRecipe::class.java)

    fun execute(param: UnfavoriteRecipeParam): Result<Unit, RecipeError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UnfavoriteRecipeParam): Result<Unit, RecipeError> {
        return success(Unit)
    }
}
