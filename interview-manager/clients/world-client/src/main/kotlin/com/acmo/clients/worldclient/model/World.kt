package com.acmo.clients.worldclient.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a world entity from the worlds table.
 */
data class World(
    val id: UUID,
    val name: String,
    val greeting: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
