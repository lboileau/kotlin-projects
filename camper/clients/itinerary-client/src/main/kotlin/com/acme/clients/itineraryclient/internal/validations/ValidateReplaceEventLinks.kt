package com.acme.clients.itineraryclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.ReplaceEventLinksParam
import org.slf4j.LoggerFactory

internal class ValidateReplaceEventLinks {
    private val logger = LoggerFactory.getLogger(ValidateReplaceEventLinks::class.java)

    fun execute(param: ReplaceEventLinksParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: ReplaceEventLinksParam): Result<Unit, AppError> {
        if (param.links.size > 10) return failure(ValidationError("links", "maximum 10 links per event"))
        if (param.links.any { it.url.isBlank() }) return failure(ValidationError("url", "must not be blank"))
        return success(Unit)
    }
}
