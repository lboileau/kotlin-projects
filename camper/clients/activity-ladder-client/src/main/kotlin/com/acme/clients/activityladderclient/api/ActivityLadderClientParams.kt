package com.acme.clients.activityladderclient.api

import com.acme.clients.activityladderclient.model.LadderBracket
import com.acme.clients.activityladderclient.model.LadderStatus
import java.math.BigDecimal
import java.util.UUID

/** Parameter for creating a new ladder in DRAFT status. */
data class CreateLadderParam(val creatorId: UUID, val title: String)

/** Parameter for fetching a ladder by its unique identifier. */
data class GetLadderByIdParam(val id: UUID)

/** Parameter for listing all ladders. Caller-side pagination via limit/offset. */
data class GetLadderListParam(val limit: Int? = null, val offset: Int? = null)

/** Parameter for adding an activity to a ladder. */
data class AddLadderActivityParam(
    val ladderId: UUID,
    val name: String,
    val imageUrl: String,
    val distanceMinutes: Int,
    val costPerPerson: BigDecimal,
)

/** Parameter for removing an activity from a ladder. */
data class RemoveLadderActivityParam(val ladderId: UUID, val activityId: UUID)

/** Parameter for listing all activities for a ladder. */
data class GetLadderActivitiesParam(val ladderId: UUID)

/** Parameter for updating ladder state fields (status, round, match, flags). */
data class UpdateLadderStateParam(
    val ladderId: UUID,
    val status: LadderStatus,
    val currentRoundNumber: Int?,
    val currentMatchActivityAId: UUID?,
    val currentMatchActivityBId: UUID?,
    val isFinalRound: Boolean,
    val isGrandFinalReset: Boolean,
)

/** Parameter for setting the winner and marking a ladder as COMPLETED. */
data class SetLadderWinnerParam(val ladderId: UUID, val winnerActivityId: UUID)

/** Parameter for restarting a ladder (resets all state transactionally). */
data class RestartLadderParam(val ladderId: UUID)

/** Parameter for updating the losses count and bracket on a single activity. */
data class UpdateActivityLossAndBracketParam(
    val activityId: UUID,
    val losses: Int,
    val bracket: LadderBracket,
)

/** Parameter for bulk-inserting participants at ladder start time. */
data class BulkInsertParticipantsParam(val ladderId: UUID, val userIds: Set<UUID>)

/** Parameter for listing participants of a ladder. */
data class GetParticipantsParam(val ladderId: UUID)

/** Parameter for casting a single vote in a round. */
data class CastVoteParam(
    val ladderId: UUID,
    val roundNumber: Int,
    val userId: UUID,
    val votedForActivityId: UUID,
)

/** Parameter for fetching all votes for a specific round. */
data class GetVotesForRoundParam(val ladderId: UUID, val roundNumber: Int)

/** Parameter for counting votes for a specific round. */
data class CountVotesForRoundParam(val ladderId: UUID, val roundNumber: Int)
