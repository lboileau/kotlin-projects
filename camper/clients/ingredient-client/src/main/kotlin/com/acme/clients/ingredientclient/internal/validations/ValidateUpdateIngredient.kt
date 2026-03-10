package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.UpdateIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateIngredient {
    private val logger = LoggerFactory.getLogger(ValidateUpdateIngredient::class.java)

    fun execute(param: UpdateIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateIngredientParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
