package com.acme.services.camperservice.features.itinerary.dto

import java.time.Instant
import java.util.UUID

data class AddEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant
)

data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant
)

data class ItineraryResponse(
    val id: UUID,
    val planId: UUID,
    val events: List<ItineraryEventResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ItineraryEventResponse(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
