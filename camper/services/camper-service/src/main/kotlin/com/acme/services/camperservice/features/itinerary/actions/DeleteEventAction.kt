package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.DeleteEventParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateDeleteEvent
import org.slf4j.LoggerFactory

internal class DeleteEventAction(private val itineraryClient: ItineraryClient) {
    private val logger = LoggerFactory.getLogger(DeleteEventAction::class.java)
    private val validate = ValidateDeleteEvent()

    fun execute(param: DeleteEventParam): Result<Unit, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        TODO()
    }
}
