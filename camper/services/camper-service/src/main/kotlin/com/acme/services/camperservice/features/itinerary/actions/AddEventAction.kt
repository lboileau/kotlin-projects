package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.params.AddEventParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateAddEvent
import org.slf4j.LoggerFactory

internal class AddEventAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(AddEventAction::class.java)
    private val validate = ValidateAddEvent()

    fun execute(param: AddEventParam): Result<ItineraryEvent, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        TODO()
    }
}
