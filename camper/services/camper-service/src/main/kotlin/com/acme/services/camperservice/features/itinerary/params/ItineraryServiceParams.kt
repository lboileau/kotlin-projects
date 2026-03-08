package com.acme.services.camperservice.features.itinerary.params

import java.time.Instant
import java.util.UUID

data class GetItineraryParam(val planId: UUID)

data class DeleteItineraryParam(val planId: UUID)

data class AddEventParam(
    val planId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant
)

data class UpdateEventParam(
    val planId: UUID,
    val eventId: UUID,
    val title: String,
    val description: String?,
    val details: String?,
    val eventAt: Instant
)

data class DeleteEventParam(val planId: UUID, val eventId: UUID)
