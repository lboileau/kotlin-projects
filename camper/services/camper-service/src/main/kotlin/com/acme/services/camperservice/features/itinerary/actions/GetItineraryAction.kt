package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.GetByPlanIdParam as ClientGetByPlanIdParam
import com.acme.clients.itineraryclient.api.GetEventsParam as ClientGetEventsParam
import com.acme.clients.itineraryclient.api.GetLinksByEventIdsParam as ClientGetLinksByEventIdsParam
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
import com.acme.services.camperservice.features.itinerary.model.Itinerary
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.model.ItineraryEventLink
import com.acme.services.camperservice.features.itinerary.params.GetItineraryParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateGetItinerary
import org.slf4j.LoggerFactory
import java.util.UUID

internal class GetItineraryAction(
    private val itineraryClient: ItineraryClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(GetItineraryAction::class.java)
    private val validate = ValidateGetItinerary()

    fun execute(param: GetItineraryParam): Result<Triple<Itinerary, List<ItineraryEvent>, Map<UUID, List<ItineraryEventLink>>>, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting itinerary for plan={}", param.planId)

        // Verify plan exists
        when (planClient.getById(PlanGetByIdParam(id = param.planId))) {
            is Result.Success -> { /* plan exists */ }
            is Result.Failure -> return Result.Failure(ItineraryError.PlanNotFound(param.planId.toString()))
        }

        // Get itinerary by planId
        val itinerary = when (val result = itineraryClient.getByPlanId(ClientGetByPlanIdParam(planId = param.planId))) {
            is Result.Success -> ItineraryMapper.fromClient(result.value)
            is Result.Failure -> return Result.Failure(ItineraryError.NotFound(param.planId.toString()))
        }

        // Get events for the itinerary (already ordered by event_at)
        val events = when (val result = itineraryClient.getEvents(ClientGetEventsParam(itineraryId = itinerary.id))) {
            is Result.Success -> result.value.map { ItineraryMapper.fromClient(it) }
            is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(result.error))
        }

        // Fetch links for all events
        val eventIds = events.map { it.id }
        val linksByEventId = if (eventIds.isNotEmpty()) {
            when (val linkResult = itineraryClient.getLinksByEventIds(
                ClientGetLinksByEventIdsParam(eventIds = eventIds)
            )) {
                is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }.groupBy { it.eventId }
                is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
            }
        } else emptyMap()

        return Result.Success(Triple(itinerary, events, linksByEventId))
    }
}
