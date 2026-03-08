package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.model.Itinerary
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.params.GetItineraryParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateGetItinerary
import org.slf4j.LoggerFactory

internal class GetItineraryAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(GetItineraryAction::class.java)
    private val validate = ValidateGetItinerary()

    fun execute(param: GetItineraryParam): Result<Pair<Itinerary, List<ItineraryEvent>>, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        TODO()
    }
}
