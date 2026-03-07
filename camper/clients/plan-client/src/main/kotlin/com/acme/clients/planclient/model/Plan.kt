package com.acme.clients.planclient.model

import java.time.Instant
import java.util.UUID

data class Plan(
    val id: UUID,
    val name: String,
    val visibility: String,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)
