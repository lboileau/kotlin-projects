package com.acme.services.camperservice.features.itinerary.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.UpdateEventParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateEvent {
    private val logger = LoggerFactory.getLogger(ValidateUpdateEvent::class.java)

    fun execute(param: UpdateEventParam): Result<Unit, ItineraryError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateEventParam): Result<Unit, ItineraryError> {
        if (param.title.isBlank()) return failure(ItineraryError.Invalid("title", "must not be blank"))
        return success(Unit)
    }
}
