package com.acme.services.camperservice.features.itinerary.mapper

import com.acme.clients.itineraryclient.model.Itinerary as ClientItinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent as ClientItineraryEvent
import com.acme.services.camperservice.features.itinerary.dto.ItineraryEventResponse
import com.acme.services.camperservice.features.itinerary.dto.ItineraryResponse
import com.acme.services.camperservice.features.itinerary.model.Itinerary
import com.acme.services.camperservice.features.itinerary.model.ItineraryEvent

object ItineraryMapper {

    fun fromClient(clientItinerary: ClientItinerary): Itinerary = Itinerary(
        id = clientItinerary.id,
        planId = clientItinerary.planId,
        createdAt = clientItinerary.createdAt,
        updatedAt = clientItinerary.updatedAt
    )

    fun fromClient(clientEvent: ClientItineraryEvent): ItineraryEvent = ItineraryEvent(
        id = clientEvent.id,
        itineraryId = clientEvent.itineraryId,
        title = clientEvent.title,
        description = clientEvent.description,
        details = clientEvent.details,
        eventAt = clientEvent.eventAt,
        category = clientEvent.category,
        estimatedCost = clientEvent.estimatedCost,
        location = clientEvent.location,
        eventEndAt = clientEvent.eventEndAt,
        createdAt = clientEvent.createdAt,
        updatedAt = clientEvent.updatedAt
    )

    fun toResponse(itinerary: Itinerary, events: List<ItineraryEvent>): ItineraryResponse = ItineraryResponse(
        id = itinerary.id,
        planId = itinerary.planId,
        events = events.map { toResponse(it) },
        totalEstimatedCost = events.mapNotNull { it.estimatedCost }.takeIf { it.isNotEmpty() }?.sumOf { it },
        createdAt = itinerary.createdAt,
        updatedAt = itinerary.updatedAt
    )

    fun toResponse(event: ItineraryEvent): ItineraryEventResponse = ItineraryEventResponse(
        id = event.id,
        itineraryId = event.itineraryId,
        title = event.title,
        description = event.description,
        details = event.details,
        eventAt = event.eventAt,
        category = event.category,
        estimatedCost = event.estimatedCost,
        location = event.location,
        eventEndAt = event.eventEndAt,
        links = emptyList(),
        createdAt = event.createdAt,
        updatedAt = event.updatedAt
    )
}
