package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.params.UpdateEventParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateUpdateEvent
import org.slf4j.LoggerFactory

internal class UpdateEventAction(private val itineraryClient: ItineraryClient) {
    private val logger = LoggerFactory.getLogger(UpdateEventAction::class.java)
    private val validate = ValidateUpdateEvent()

    fun execute(param: UpdateEventParam): Result<ItineraryEvent, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        TODO()
    }
}
