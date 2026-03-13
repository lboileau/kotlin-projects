package com.acme.clients.logbookclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.CreateFaqParam
import org.slf4j.LoggerFactory

internal class ValidateCreateFaq {
    private val logger = LoggerFactory.getLogger(ValidateCreateFaq::class.java)

    fun execute(param: CreateFaqParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateFaqParam): Result<Unit, AppError> {
        if (param.question.isBlank()) return failure(ValidationError("question", "must not be blank"))
        return success(Unit)
    }
}
