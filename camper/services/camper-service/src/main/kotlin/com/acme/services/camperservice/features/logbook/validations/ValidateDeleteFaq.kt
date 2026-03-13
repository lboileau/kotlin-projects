package com.acme.services.camperservice.features.logbook.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.DeleteFaqParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteFaq {
    private val logger = LoggerFactory.getLogger(ValidateDeleteFaq::class.java)

    fun execute(param: DeleteFaqParam): Result<Unit, LogBookError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: DeleteFaqParam): Result<Unit, LogBookError> {
        return success(Unit)
    }
}
