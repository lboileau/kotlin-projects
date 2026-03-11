package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import org.slf4j.LoggerFactory

internal class ValidateAddRecipeIngredients {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipeIngredients::class.java)
    private val validateOne = ValidateAddRecipeIngredient()

    fun execute(param: AddRecipeIngredientsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipeIngredientsParam): Result<Unit, AppError> {
        for (ingredient in param.ingredients) {
            val result = validateOne.execute(ingredient)
            if (result is Result.Failure) return result
        }
        return success(Unit)
    }
}
