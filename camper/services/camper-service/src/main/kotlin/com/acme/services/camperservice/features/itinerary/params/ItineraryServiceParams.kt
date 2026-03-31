package com.acme.services.camperservice.features.itinerary.params

import com.acme.services.camperservice.features.itinerary.dto.LinkInput
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class GetItineraryParam(val planId: UUID)

data class DeleteItineraryParam(val planId: UUID)

data class AddEventParam(
    val planId: UUID,
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

data class UpdateEventParam(
    val planId: UUID,
    val eventId: UUID,
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

data class DeleteEventParam(val planId: UUID, val eventId: UUID)
