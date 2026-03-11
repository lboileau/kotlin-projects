package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.FindByNamesParam
import org.slf4j.LoggerFactory

internal class ValidateFindIngredientsByNames {
    private val logger = LoggerFactory.getLogger(ValidateFindIngredientsByNames::class.java)

    fun execute(param: FindByNamesParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: FindByNamesParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
