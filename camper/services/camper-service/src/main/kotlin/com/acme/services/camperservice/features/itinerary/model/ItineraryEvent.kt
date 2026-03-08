package com.acme.services.camperservice.features.itinerary.model

import java.time.Instant
import java.util.UUID

data class ItineraryEvent(
    val id: UUID,
    val itineraryId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
