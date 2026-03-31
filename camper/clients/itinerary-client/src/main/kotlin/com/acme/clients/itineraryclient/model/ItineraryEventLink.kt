package com.acme.clients.itineraryclient.model

import java.time.Instant
import java.util.UUID

data class ItineraryEventLink(
    val id: UUID,
    val eventId: UUID,
    val url: String,
    val label: String?,
    val createdAt: Instant
)
