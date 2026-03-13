package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.AnswerFaqParam
import org.slf4j.LoggerFactory

internal class ValidateAnswerFaq {
    private val logger = LoggerFactory.getLogger(ValidateAnswerFaq::class.java)

    fun execute(param: AnswerFaqParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AnswerFaqParam): Result<Unit, LogBookError> {
        if (param.answer.isBlank()) return failure(LogBookError.Invalid("answer", "must not be blank"))
        return success(Unit)
    }
}
