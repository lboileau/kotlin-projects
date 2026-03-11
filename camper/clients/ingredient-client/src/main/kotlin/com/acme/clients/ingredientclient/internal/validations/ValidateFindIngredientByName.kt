package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.FindByNameParam
import org.slf4j.LoggerFactory

internal class ValidateFindIngredientByName {
    private val logger = LoggerFactory.getLogger(ValidateFindIngredientByName::class.java)

    fun execute(param: FindByNameParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: FindByNameParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
