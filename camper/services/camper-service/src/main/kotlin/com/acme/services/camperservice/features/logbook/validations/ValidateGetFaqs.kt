package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.GetFaqsParam
import org.slf4j.LoggerFactory

internal class ValidateGetFaqs {
    private val logger = LoggerFactory.getLogger(ValidateGetFaqs::class.java)

    fun execute(param: GetFaqsParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: GetFaqsParam): Result<Unit, LogBookError> {
        return success(Unit)
    }
}
