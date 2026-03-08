package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.params.DeleteItineraryParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateDeleteItinerary
import org.slf4j.LoggerFactory

internal class DeleteItineraryAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(DeleteItineraryAction::class.java)
    private val validate = ValidateDeleteItinerary()

    fun execute(param: DeleteItineraryParam): Result<Unit, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        TODO()
    }
}
