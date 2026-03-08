package com.acme.clients.assignmentclient.model

import java.time.Instant
import java.util.UUID

data class Assignment(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val type: String,
    val maxOccupancy: Int,
    val ownerId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)
