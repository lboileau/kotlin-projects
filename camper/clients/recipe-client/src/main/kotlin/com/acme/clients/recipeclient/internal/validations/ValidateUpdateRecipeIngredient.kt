package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.UpdateRecipeIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateRecipeIngredient {
    private val logger = LoggerFactory.getLogger(ValidateUpdateRecipeIngredient::class.java)

    fun execute(param: UpdateRecipeIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateRecipeIngredientParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
