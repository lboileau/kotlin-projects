package com.acme.services.camperservice.features.itinerary.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LinkInput(
    val url: String,
    val label: String?
)

data class LinkResponse(
    val id: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)

data class AddEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant,
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkInput>?
)

data class ItineraryResponse(
    val id: UUID,
    val planId: UUID,
    val events: List<ItineraryEventResponse>,
    val totalEstimatedCost: BigDecimal?,
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
    val category: String,
    val estimatedCost: BigDecimal?,
    val location: String?,
    val eventEndAt: Instant?,
    val links: List<LinkResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)
