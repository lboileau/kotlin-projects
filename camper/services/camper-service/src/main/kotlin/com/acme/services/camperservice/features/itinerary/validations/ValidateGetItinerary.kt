package com.acme.services.camperservice.features.itinerary.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.GetItineraryParam
import org.slf4j.LoggerFactory

internal class ValidateGetItinerary {
    private val logger = LoggerFactory.getLogger(ValidateGetItinerary::class.java)

    fun execute(param: GetItineraryParam): Result<Unit, ItineraryError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetItineraryParam): Result<Unit, ItineraryError> {
        return success(Unit)
    }
}
