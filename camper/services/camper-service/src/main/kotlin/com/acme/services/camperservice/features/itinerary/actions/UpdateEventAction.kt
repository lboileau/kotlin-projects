package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.UpdateEventParam as ClientUpdateEventParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
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

        logger.debug("Updating event={} for plan={}", param.eventId, param.planId)

        return when (val result = itineraryClient.updateEvent(
            ClientUpdateEventParam(
                id = param.eventId,
                title = param.title,
                description = param.description,
                details = param.details,
                eventAt = param.eventAt
            )
        )) {
            is Result.Success -> Result.Success(ItineraryMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(ItineraryError.EventNotFound(param.eventId.toString()))
        }
    }
}
