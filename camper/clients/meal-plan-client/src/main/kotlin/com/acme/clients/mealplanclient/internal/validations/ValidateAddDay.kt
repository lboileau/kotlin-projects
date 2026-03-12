package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddDayParam
import org.slf4j.LoggerFactory

internal class ValidateAddDay {
    private val logger = LoggerFactory.getLogger(ValidateAddDay::class.java)

    fun execute(param: AddDayParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddDayParam): Result<Unit, AppError> {
        if (param.dayNumber <= 0) return failure(ValidationError("dayNumber", "must be greater than 0"))
        return success(Unit)
    }
}
