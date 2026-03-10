package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.CreateIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateCreateIngredient {
    private val logger = LoggerFactory.getLogger(ValidateCreateIngredient::class.java)

    fun execute(param: CreateIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateIngredientParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
