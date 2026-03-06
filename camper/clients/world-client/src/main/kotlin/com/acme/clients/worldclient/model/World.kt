package com.acme.clients.worldclient.model

import java.time.Instant
import java.util.UUID

data class World(
    val id: UUID,
    val name: String,
    val greeting: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
