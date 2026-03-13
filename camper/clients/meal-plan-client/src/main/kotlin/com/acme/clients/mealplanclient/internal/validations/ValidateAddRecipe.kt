package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddRecipeParam
import org.slf4j.LoggerFactory

internal class ValidateAddRecipe {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipe::class.java)

    fun execute(param: AddRecipeParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipeParam): Result<Unit, AppError> {
        if (param.mealType !in VALID_MEAL_TYPES) return failure(ValidationError("mealType", "must be one of: ${VALID_MEAL_TYPES.joinToString()}"))
        return success(Unit)
    }

    companion object {
        val VALID_MEAL_TYPES = setOf("breakfast", "lunch", "dinner", "snack")
    }
}
