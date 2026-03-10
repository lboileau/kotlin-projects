package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateAddRecipeIngredient {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipeIngredient::class.java)

    fun execute(param: AddRecipeIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipeIngredientParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
