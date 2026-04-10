package com.acme.services.camperservice.features.activityladder.model

import java.util.UUID

/** Enriched participant with user profile data fetched at read time. */
data class EnrichedParticipant(
    val userId: UUID,
    val username: String?,
    val avatarSeed: String?,
)

/** Current round state for an ACTIVE ladder. */
data class CurrentRound(
    val roundNumber: Int,
    val activityAId: UUID,
    val activityBId: UUID,
    val votesCast: Int,
    val totalVoters: Int,
    val votedUserIds: List<UUID>,
)

/** Aggregate view of a ladder with all related data for the detail endpoint. */
data class LadderDetail(
    val ladder: Ladder,
    val activities: List<LadderActivity>,
    val participants: List<EnrichedParticipant>,
    val currentRound: CurrentRound?,
)
