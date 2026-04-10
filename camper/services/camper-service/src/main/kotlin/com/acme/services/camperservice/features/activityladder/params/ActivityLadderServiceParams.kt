package com.acme.services.camperservice.features.activityladder.params

import java.math.BigDecimal
import java.util.UUID

data class NewActivityInput(
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)

data class CreateLadderParam(
    val requestingUserId: UUID,
    val title: String,
    val activities: List<NewActivityInput>,
)

data class ListLaddersParam(
    val requestingUserId: UUID,
)

data class GetLadderDetailParam(
    val ladderId: UUID,
    val requestingUserId: UUID,
)

data class AddActivityParam(
    val ladderId: UUID,
    val requestingUserId: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)

data class RemoveActivityParam(
    val ladderId: UUID,
    val activityId: UUID,
    val requestingUserId: UUID,
)

data class StartLadderParam(
    val ladderId: UUID,
    val requestingUserId: UUID,
    val presentUserIds: Set<UUID>,
)

data class CastVoteParam(
    val ladderId: UUID,
    val requestingUserId: UUID,
    val votedForActivityId: UUID,
)

data class RestartLadderParam(
    val ladderId: UUID,
    val requestingUserId: UUID,
)
