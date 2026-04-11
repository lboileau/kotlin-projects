package com.acme.services.camperservice.features.activityladder.dto

import java.util.UUID

data class CurrentRoundView(
    val roundNumber: Int,
    val activityAId: UUID,
    val activityBId: UUID,
    val votesCast: Int,
    val totalVoters: Int,
    val votedUserIds: List<UUID>,
)
