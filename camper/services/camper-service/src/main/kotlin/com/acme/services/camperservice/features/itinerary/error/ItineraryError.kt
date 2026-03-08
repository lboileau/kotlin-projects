package com.acme.services.camperservice.features.itinerary.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError

sealed class ItineraryError(override val message: String) : AppError {
    data class PlanNotFound(val planId: String) : ItineraryError("Plan not found: $planId")
    data class NotFound(val planId: String) : ItineraryError("Itinerary not found for plan: $planId")
    data class EventNotFound(val eventId: String) : ItineraryError("Itinerary event not found: $eventId")
    data class Invalid(val field: String, val reason: String) : ItineraryError("Invalid $field: $reason")

    companion object {
        fun fromClientError(error: AppError): ItineraryError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
