package com.acme.services.camperservice.features.recipe.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.FavoriteRecipeParam
import org.slf4j.LoggerFactory

internal class ValidateFavoriteRecipe {
    private val logger = LoggerFactory.getLogger(ValidateFavoriteRecipe::class.java)

    fun execute(param: FavoriteRecipeParam): Result<Unit, RecipeError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: FavoriteRecipeParam): Result<Unit, RecipeError> {
        return success(Unit)
    }
}
