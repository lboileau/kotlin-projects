package com.acme.clients.itineraryclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.UpdateEventParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateEvent {
    private val logger = LoggerFactory.getLogger(ValidateUpdateEvent::class.java)

    fun execute(param: UpdateEventParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateEventParam): Result<Unit, AppError> {
        if (param.title.isBlank()) return failure(ValidationError("title", "must not be blank"))
        return success(Unit)
    }
}
