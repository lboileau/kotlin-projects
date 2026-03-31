package com.acme.clients.itineraryclient.api

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Parameter for retrieving an itinerary by plan ID. */
data class GetByPlanIdParam(val planId: UUID)

/** Parameter for creating a new itinerary. */
data class CreateItineraryParam(val planId: UUID)

/** Parameter for deleting an itinerary. */
data class DeleteItineraryParam(val planId: UUID)

/** Parameter for retrieving events of an itinerary. */
data class GetEventsParam(val itineraryId: UUID)

/** Parameter for adding an event to an itinerary. */
data class AddEventParam(
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?
)

/** Parameter for updating an itinerary event. */
data class UpdateEventParam(
    val id: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?
)

/** Parameter for deleting an itinerary event. */
data class DeleteEventParam(val id: UUID)

/** Parameter for retrieving links by event IDs. */
data class GetLinksByEventIdsParam(val eventIds: List<UUID>)

/** Input for a single link to create. */
data class LinkInput(val url: String, val label: String?)

/** Parameter for replacing all links on an event. */
data class ReplaceEventLinksParam(val eventId: UUID, val links: List<LinkInput>)
