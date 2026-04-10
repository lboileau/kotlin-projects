package com.acme.clients.activityladderclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
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
import com.acme.clients.activityladderclient.model.LadderParticipant
import com.acme.clients.activityladderclient.model.LadderVote
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeActivityLadderClient : ActivityLadderClient {

    private val ladderStore = ConcurrentHashMap<UUID, Ladder>()
    private val activityStore = ConcurrentHashMap<UUID, LadderActivity>()
    private val participantStore = ConcurrentHashMap<Pair<UUID, UUID>, LadderParticipant>()
    private val voteStore = ConcurrentHashMap<Triple<UUID, Int, UUID>, LadderVote>()
    private val ladderLocks = ConcurrentHashMap<UUID, Any>()

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
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun castVote(param: CastVoteParam): Result<LadderVote, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }

    override fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError> {
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
