package com.acme.services.camperservice.features.itinerary.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.AddEventParam
import org.slf4j.LoggerFactory

internal class ValidateAddEvent {
    private val logger = LoggerFactory.getLogger(ValidateAddEvent::class.java)

    fun execute(param: AddEventParam): Result<Unit, ItineraryError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddEventParam): Result<Unit, ItineraryError> {
        if (param.title.isBlank()) return failure(ItineraryError.Invalid("title", "must not be blank"))
        if (param.category !in VALID_CATEGORIES) return failure(ItineraryError.Invalid("category", "must be one of: $VALID_CATEGORIES_STRING"))
        if (param.estimatedCost != null && param.estimatedCost < java.math.BigDecimal.ZERO) return failure(ItineraryError.Invalid("estimatedCost", "must be >= 0"))
        if (param.eventEndAt != null && !param.eventEndAt.isAfter(param.eventAt)) return failure(ItineraryError.Invalid("eventEndAt", "must be after eventAt"))
        if (param.links != null && param.links.size > 10) return failure(ItineraryError.Invalid("links", "maximum 10 links per event"))
        if (param.links != null && param.links.any { it.url.isBlank() }) return failure(ItineraryError.Invalid("url", "must not be blank"))
        return success(Unit)
    }

    companion object {
        private val VALID_CATEGORIES = setOf("travel", "accommodation", "activity", "meal", "other")
        private val VALID_CATEGORIES_STRING = VALID_CATEGORIES.joinToString(", ")
    }
}
