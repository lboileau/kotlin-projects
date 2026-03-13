package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.AskFaqParam
import org.slf4j.LoggerFactory

internal class ValidateAskFaq {
    private val logger = LoggerFactory.getLogger(ValidateAskFaq::class.java)

    fun execute(param: AskFaqParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AskFaqParam): Result<Unit, LogBookError> {
        if (param.question.isBlank()) return failure(LogBookError.Invalid("question", "must not be blank"))
        return success(Unit)
    }
}
