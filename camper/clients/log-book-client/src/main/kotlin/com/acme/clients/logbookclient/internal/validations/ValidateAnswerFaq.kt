package com.acme.clients.logbookclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.logbookclient.api.AnswerFaqParam
import org.slf4j.LoggerFactory

internal class ValidateAnswerFaq {
    private val logger = LoggerFactory.getLogger(ValidateAnswerFaq::class.java)

    fun execute(param: AnswerFaqParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AnswerFaqParam): Result<Unit, AppError> {
        if (param.answer.isBlank()) return failure(ValidationError("answer", "must not be blank"))
        return success(Unit)
    }
}
