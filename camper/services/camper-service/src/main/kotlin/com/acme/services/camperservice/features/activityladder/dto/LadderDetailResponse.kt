package com.acme.services.camperservice.features.activityladder.dto

import java.time.Instant
import java.util.UUID

data class LadderDetailResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val creatorId: UUID,
    val activities: List<LadderActivityResponse>,
    val participants: List<LadderParticipantResponse>,
    val currentRound: CurrentRoundView?,
    val winnerActivityId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
