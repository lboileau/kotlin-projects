package com.acme.services.camperservice.features.itinerary.actions

import com.acme.clients.common.Result
import com.acme.clients.itineraryclient.api.GetLinksByEventIdsParam as ClientGetLinksByEventIdsParam
import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.api.LinkInput as ClientLinkInput
import com.acme.clients.itineraryclient.api.ReplaceEventLinksParam as ClientReplaceEventLinksParam
import com.acme.clients.itineraryclient.api.UpdateEventParam as ClientUpdateEventParam
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent
import com.acme.services.camperservice.features.itinerary.model.ItineraryEventLink
import com.acme.services.camperservice.features.itinerary.params.UpdateEventParam
import com.acme.services.camperservice.features.itinerary.validations.ValidateUpdateEvent
import org.slf4j.LoggerFactory

internal class UpdateEventAction(private val itineraryClient: ItineraryClient) {
    private val logger = LoggerFactory.getLogger(UpdateEventAction::class.java)
    private val validate = ValidateUpdateEvent()

    fun execute(param: UpdateEventParam): Result<Pair<ItineraryEvent, List<ItineraryEventLink>>, ItineraryError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating event={} for plan={}", param.eventId, param.planId)

        val event = when (val result = itineraryClient.updateEvent(
            ClientUpdateEventParam(
                id = param.eventId,
                title = param.title,
                description = param.description,
                details = param.details,
                eventAt = param.eventAt,
                category = param.category,
                estimatedCost = param.estimatedCost,
                location = param.location,
                eventEndAt = param.eventEndAt
            )
        )) {
            is Result.Success -> ItineraryMapper.fromClient(result.value)
            is Result.Failure -> return Result.Failure(ItineraryError.EventNotFound(param.eventId.toString()))
        }

        // Handle links: replace if provided, fetch existing if null
        val links = if (param.links != null) {
            when (val linkResult = itineraryClient.replaceEventLinks(
                ClientReplaceEventLinksParam(
                    eventId = param.eventId,
                    links = param.links.map { ClientLinkInput(url = it.url, label = it.label) }
                )
            )) {
                is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
                is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
            }
        } else {
            // links is null → don't touch existing links, fetch current ones
            when (val linkResult = itineraryClient.getLinksByEventIds(
                ClientGetLinksByEventIdsParam(eventIds = listOf(param.eventId))
            )) {
                is Result.Success -> linkResult.value.map { ItineraryMapper.fromClient(it) }
                is Result.Failure -> return Result.Failure(ItineraryError.fromClientError(linkResult.error))
            }
        }

        return Result.Success(Pair(event, links))
    }
}
