package com.acme.clients.activityladderclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.AddLadderActivityParam
import com.acme.clients.activityladderclient.api.BulkInsertParticipantsParam
import com.acme.clients.activityladderclient.api.CastVoteParam
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.api.CreateLadderParam
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.GetLadderListParam
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.activityladderclient.api.RemoveLadderActivityParam
import com.acme.clients.activityladderclient.api.RestartLadderParam
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import com.acme.clients.activityladderclient.api.UpdateActivityLossAndBracketParam
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.internal.validations.ValidateAddLadderActivity
import com.acme.clients.activityladderclient.internal.validations.ValidateBulkInsertParticipants
import com.acme.clients.activityladderclient.internal.validations.ValidateCastVote
import com.acme.clients.activityladderclient.internal.validations.ValidateCountVotesForRound
import com.acme.clients.activityladderclient.internal.validations.ValidateCreateLadder
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderActivities
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderById
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderList
import com.acme.clients.activityladderclient.internal.validations.ValidateGetParticipants
import com.acme.clients.activityladderclient.internal.validations.ValidateGetVotesForRound
import com.acme.clients.activityladderclient.internal.validations.ValidateRemoveLadderActivity
import com.acme.clients.activityladderclient.internal.validations.ValidateRestartLadder
import com.acme.clients.activityladderclient.internal.validations.ValidateSetLadderWinner
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateActivityLossAndBracket
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateLadderState
import com.acme.clients.activityladderclient.model.Ladder
import com.acme.clients.activityladderclient.model.LadderActivity
import com.acme.clients.activityladderclient.model.LadderBracket
import com.acme.clients.activityladderclient.model.LadderParticipant
import com.acme.clients.activityladderclient.model.LadderStatus
import com.acme.clients.activityladderclient.model.LadderVote
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeActivityLadderClient : ActivityLadderClient {

    private val ladderStore = ConcurrentHashMap<UUID, Ladder>()
    private val activityStore = ConcurrentHashMap<UUID, LadderActivity>()
    private val participantStore = ConcurrentHashMap<Pair<UUID, UUID>, LadderParticipant>()
    private val voteStore = ConcurrentHashMap<Triple<UUID, Int, UUID>, LadderVote>()
    private val ladderLocks = ConcurrentHashMap<UUID, Any>()

    private companion object {
        const val MAX_LIMIT = 200
    }

    private val validateCreateLadder = ValidateCreateLadder()
    private val validateGetLadderById = ValidateGetLadderById()
    private val validateGetLadderList = ValidateGetLadderList()
    private val validateAddLadderActivity = ValidateAddLadderActivity()
    private val validateRemoveLadderActivity = ValidateRemoveLadderActivity()
    private val validateGetLadderActivities = ValidateGetLadderActivities()
    private val validateUpdateLadderState = ValidateUpdateLadderState()
    private val validateSetLadderWinner = ValidateSetLadderWinner()
    private val validateRestartLadder = ValidateRestartLadder()
    private val validateUpdateActivityLossAndBracket = ValidateUpdateActivityLossAndBracket()
    private val validateBulkInsertParticipants = ValidateBulkInsertParticipants()
    private val validateGetParticipants = ValidateGetParticipants()
    private val validateCastVote = ValidateCastVote()
    private val validateGetVotesForRound = ValidateGetVotesForRound()
    private val validateCountVotesForRound = ValidateCountVotesForRound()

    override fun createLadder(param: CreateLadderParam): Result<Ladder, AppError> {
        val validation = validateCreateLadder.execute(param)
        if (validation is Result.Failure) return validation

        val ladder = Ladder(
            id = UUID.randomUUID(),
            creatorId = param.creatorId,
            title = param.title,
            status = LadderStatus.DRAFT,
            currentRoundNumber = null,
            currentMatchActivityAId = null,
            currentMatchActivityBId = null,
            isFinalRound = false,
            isGrandFinalReset = false,
            winnerActivityId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        ladderStore[ladder.id] = ladder
        return success(ladder)
    }

    override fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError> {
        val validation = validateGetLadderById.execute(param)
        if (validation is Result.Failure) return validation

        val ladder = ladderStore[param.id]
        return if (ladder != null) success(ladder) else failure(NotFoundError("Ladder", param.id.toString()))
    }

    override fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError> {
        val validation = validateGetLadderList.execute(param)
        if (validation is Result.Failure) return validation

        val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)
        var ladders = ladderStore.values.sortedByDescending { it.createdAt }
        if (param.offset != null) ladders = ladders.drop(param.offset)
        ladders = ladders.take(effectiveLimit)
        return success(ladders)
    }

    override fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError> {
        val validation = validateAddLadderActivity.execute(param)
        if (validation is Result.Failure) return validation

        val maxOrder = activityStore.values
            .filter { it.ladderId == param.ladderId }
            .maxOfOrNull { it.displayOrder } ?: 0

        val activity = LadderActivity(
            id = UUID.randomUUID(),
            ladderId = param.ladderId,
            name = param.name,
            imageUrl = param.imageUrl,
            distanceMinutes = param.distanceMinutes,
            costPerPerson = param.costPerPerson,
            losses = 0,
            bracket = LadderBracket.WINNERS,
            displayOrder = maxOrder + 1,
            createdAt = Instant.now(),
        )
        activityStore[activity.id] = activity
        return success(activity)
    }

    override fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError> {
        val validation = validateRemoveLadderActivity.execute(param)
        if (validation is Result.Failure) return validation

        val existing = activityStore[param.activityId]
        return if (existing != null && existing.ladderId == param.ladderId) {
            activityStore.remove(param.activityId)
            success(Unit)
        } else {
            failure(NotFoundError("LadderActivity", param.activityId.toString()))
        }
    }

    override fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> {
        val validation = validateGetLadderActivities.execute(param)
        if (validation is Result.Failure) return validation

        val activities = activityStore.values
            .filter { it.ladderId == param.ladderId }
            .sortedBy { it.displayOrder }
        return success(activities)
    }

    override fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError> {
        val validation = validateUpdateLadderState.execute(param)
        if (validation is Result.Failure) return validation

        val existing = ladderStore[param.ladderId]
            ?: return failure(NotFoundError("Ladder", param.ladderId.toString()))

        val updated = existing.copy(
            status = param.status,
            currentRoundNumber = param.currentRoundNumber,
            currentMatchActivityAId = param.currentMatchActivityAId,
            currentMatchActivityBId = param.currentMatchActivityBId,
            isFinalRound = param.isFinalRound,
            isGrandFinalReset = param.isGrandFinalReset,
            updatedAt = Instant.now(),
        )
        ladderStore[param.ladderId] = updated
        return success(updated)
    }

    override fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError> {
        val validation = validateSetLadderWinner.execute(param)
        if (validation is Result.Failure) return validation

        val existing = ladderStore[param.ladderId]
            ?: return failure(NotFoundError("Ladder", param.ladderId.toString()))

        val updated = existing.copy(
            status = LadderStatus.COMPLETED,
            winnerActivityId = param.winnerActivityId,
            updatedAt = Instant.now(),
        )
        ladderStore[param.ladderId] = updated
        return success(updated)
    }

    override fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError> {
        val validation = validateRestartLadder.execute(param)
        if (validation is Result.Failure) return validation

        val existing = ladderStore[param.ladderId]
            ?: return failure(NotFoundError("Ladder", param.ladderId.toString()))

        // Delete votes for this ladder
        voteStore.keys.filter { it.first == param.ladderId }.forEach { voteStore.remove(it) }

        // Delete participants for this ladder
        participantStore.keys.filter { it.first == param.ladderId }.forEach { participantStore.remove(it) }

        // Reset activities to 0 losses and WINNERS bracket
        activityStore.entries
            .filter { it.value.ladderId == param.ladderId }
            .forEach { (id, activity) ->
                activityStore[id] = activity.copy(losses = 0, bracket = LadderBracket.WINNERS)
            }

        // Reset ladder state to DRAFT
        val updated = existing.copy(
            status = LadderStatus.DRAFT,
            currentRoundNumber = null,
            currentMatchActivityAId = null,
            currentMatchActivityBId = null,
            isFinalRound = false,
            isGrandFinalReset = false,
            winnerActivityId = null,
            updatedAt = Instant.now(),
        )
        ladderStore[param.ladderId] = updated
        return success(updated)
    }

    override fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> {
        val validation = validateUpdateActivityLossAndBracket.execute(param)
        if (validation is Result.Failure) return validation

        val existing = activityStore[param.activityId]
            ?: return failure(NotFoundError("LadderActivity", param.activityId.toString()))

        val updated = existing.copy(
            losses = param.losses,
            bracket = param.bracket,
        )
        activityStore[param.activityId] = updated
        return success(updated)
    }

    override fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> {
        val validation = validateBulkInsertParticipants.execute(param)
        if (validation is Result.Failure) return validation

        for (userId in param.userIds) {
            val key = Pair(param.ladderId, userId)
            if (!participantStore.containsKey(key)) {
                val participant = LadderParticipant(
                    id = UUID.randomUUID(),
                    ladderId = param.ladderId,
                    userId = userId,
                    createdAt = Instant.now(),
                )
                participantStore[key] = participant
            }
        }

        val participants = participantStore.values.filter { it.ladderId == param.ladderId }
        return success(participants)
    }

    override fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> {
        val validation = validateGetParticipants.execute(param)
        if (validation is Result.Failure) return validation

        val participants = participantStore.values.filter { it.ladderId == param.ladderId }
        return success(participants)
    }

    override fun castVote(param: CastVoteParam): Result<LadderVote, AppError> {
        val validation = validateCastVote.execute(param)
        if (validation is Result.Failure) return validation

        val key = Triple(param.ladderId, param.roundNumber, param.userId)
        if (voteStore.containsKey(key)) {
            return failure(ConflictError("LadderVote", "User ${param.userId} already voted in round ${param.roundNumber}"))
        }

        val vote = LadderVote(
            id = UUID.randomUUID(),
            ladderId = param.ladderId,
            roundNumber = param.roundNumber,
            userId = param.userId,
            votedForActivityId = param.votedForActivityId,
            createdAt = Instant.now(),
        )
        voteStore[key] = vote
        return success(vote)
    }

    override fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> {
        val validation = validateGetVotesForRound.execute(param)
        if (validation is Result.Failure) return validation

        val votes = voteStore.values.filter {
            it.ladderId == param.ladderId && it.roundNumber == param.roundNumber
        }
        return success(votes)
    }

    override fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError> {
        val validation = validateCountVotesForRound.execute(param)
        if (validation is Result.Failure) return validation

        val count = voteStore.values.count {
            it.ladderId == param.ladderId && it.roundNumber == param.roundNumber
        }
        return success(count)
    }

    override fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError> {
        if (!ladderStore.containsKey(ladderId)) {
            return failure(NotFoundError("Ladder", ladderId.toString()))
        }
        val lock = ladderLocks.getOrPut(ladderId) { Any() }
        return synchronized(lock) {
            block(this)
        }
    }

    fun reset() {
        ladderStore.clear()
        activityStore.clear()
        participantStore.clear()
        voteStore.clear()
        ladderLocks.clear()
    }

    fun seed(vararg ladders: Ladder) {
        ladders.forEach { ladderStore[it.id] = it }
    }

    fun seedActivities(vararg activities: LadderActivity) {
        activities.forEach { activityStore[it.id] = it }
    }

    fun seedParticipants(vararg participants: LadderParticipant) {
        participants.forEach { participantStore[Pair(it.ladderId, it.userId)] = it }
    }

    fun seedVotes(vararg votes: LadderVote) {
        votes.forEach { voteStore[Triple(it.ladderId, it.roundNumber, it.userId)] = it }
    }
}
