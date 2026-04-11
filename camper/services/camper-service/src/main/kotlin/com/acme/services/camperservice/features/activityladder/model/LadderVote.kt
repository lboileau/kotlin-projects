package com.acme.services.camperservice.features.activityladder.model

import java.time.Instant
import java.util.UUID

data class LadderVote(
    val id: UUID,
    val ladderId: UUID,
    val roundNumber: Int,
    val userId: UUID,
    val votedForActivityId: UUID,
    val createdAt: Instant,
)
