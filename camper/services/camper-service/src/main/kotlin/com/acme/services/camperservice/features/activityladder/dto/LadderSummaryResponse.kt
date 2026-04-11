package com.acme.services.camperservice.features.activityladder.dto

import java.time.Instant
import java.util.UUID

data class LadderSummaryResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val creatorId: UUID,
    val activityCount: Int,
    val participantCount: Int,
    val winnerActivityId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ListLaddersResponse(
    val ladders: List<LadderSummaryResponse>,
)
