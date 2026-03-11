package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateGetAllIngredients {
    private val logger = LoggerFactory.getLogger(ValidateGetAllIngredients::class.java)

    fun execute(): Result<Unit, AppError> {
        return validate().also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(): Result<Unit, AppError> {
        return success(Unit)
    }
}
