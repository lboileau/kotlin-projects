package com.acmo.services.interviewservice.features.world.dto

import java.time.Instant
import java.util.UUID

data class WorldResponse(
    val id: UUID,
    val name: String,
    val greeting: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
