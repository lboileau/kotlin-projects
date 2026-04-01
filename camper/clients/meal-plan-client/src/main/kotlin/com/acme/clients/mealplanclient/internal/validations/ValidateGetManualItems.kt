package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetManualItemsParam
import org.slf4j.LoggerFactory

internal class ValidateGetManualItems {
    private val logger = LoggerFactory.getLogger(ValidateGetManualItems::class.java)

    fun execute(param: GetManualItemsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetManualItemsParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
