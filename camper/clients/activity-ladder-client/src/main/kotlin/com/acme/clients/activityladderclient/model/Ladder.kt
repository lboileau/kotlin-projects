package com.acme.clients.activityladderclient.model

import java.time.Instant
import java.util.UUID

data class Ladder(
    val id: UUID,
    val creatorId: UUID,
    val title: String,
    val status: LadderStatus,
    val currentRoundNumber: Int?,
    val currentMatchActivityAId: UUID?,
    val currentMatchActivityBId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
    val winnerActivityId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
