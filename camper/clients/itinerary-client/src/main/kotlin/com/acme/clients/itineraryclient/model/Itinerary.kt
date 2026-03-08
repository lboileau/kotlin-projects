package com.acme.clients.itineraryclient.model

import java.time.Instant
import java.util.UUID

data class Itinerary(
    val id: UUID,
    val planId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)
