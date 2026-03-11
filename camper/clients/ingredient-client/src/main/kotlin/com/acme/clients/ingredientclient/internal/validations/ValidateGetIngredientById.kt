package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.GetByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetIngredientById {
    private val logger = LoggerFactory.getLogger(ValidateGetIngredientById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
