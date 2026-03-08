package com.acme.services.camperservice.features.itinerary.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.DeleteEventParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteEvent {
    private val logger = LoggerFactory.getLogger(ValidateDeleteEvent::class.java)

    fun execute(param: DeleteEventParam): Result<Unit, ItineraryError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: DeleteEventParam): Result<Unit, ItineraryError> {
        return success(Unit)
    }
}
