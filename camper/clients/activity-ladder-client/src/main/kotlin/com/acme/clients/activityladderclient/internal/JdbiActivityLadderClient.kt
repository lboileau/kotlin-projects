package com.acme.clients.activityladderclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
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
import com.acme.clients.activityladderclient.internal.operations.AddLadderActivity
import com.acme.clients.activityladderclient.internal.operations.BulkInsertParticipants
import com.acme.clients.activityladderclient.internal.operations.CastVote
import com.acme.clients.activityladderclient.internal.operations.CountVotesForRound
import com.acme.clients.activityladderclient.internal.operations.CreateLadder
import com.acme.clients.activityladderclient.internal.operations.GetLadderActivities
import com.acme.clients.activityladderclient.internal.operations.GetLadderById
import com.acme.clients.activityladderclient.internal.operations.GetLadderList
import com.acme.clients.activityladderclient.internal.operations.GetParticipants
import com.acme.clients.activityladderclient.internal.operations.GetVotesForRound
import com.acme.clients.activityladderclient.internal.operations.RemoveLadderActivity
import com.acme.clients.activityladderclient.internal.operations.RestartLadder
import com.acme.clients.activityladderclient.internal.operations.SetLadderWinner
import com.acme.clients.activityladderclient.internal.operations.UpdateActivityLossAndBracket
import com.acme.clients.activityladderclient.internal.operations.UpdateLadderStatus
import com.acme.clients.activityladderclient.model.Ladder
import com.acme.clients.activityladderclient.model.LadderActivity
import com.acme.clients.activityladderclient.model.LadderParticipant
import com.acme.clients.activityladderclient.model.LadderVote
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.util.UUID

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiActivityLadderClient(private val jdbi: Jdbi) : ActivityLadderClient {

    private val createLadder = CreateLadder(jdbi)
    private val getLadderById = GetLadderById(jdbi)
    private val getLadderList = GetLadderList(jdbi)
    private val addLadderActivity = AddLadderActivity(jdbi)
    private val removeLadderActivity = RemoveLadderActivity(jdbi)
    private val getLadderActivities = GetLadderActivities(jdbi)
    private val updateLadderStatus = UpdateLadderStatus(jdbi)
    private val setLadderWinner = SetLadderWinner(jdbi)
    private val restartLadder = RestartLadder(jdbi)
    private val updateActivityLossAndBracket = UpdateActivityLossAndBracket(jdbi)
    private val bulkInsertParticipants = BulkInsertParticipants(jdbi)
    private val getParticipants = GetParticipants(jdbi)
    private val castVote = CastVote(jdbi)
    private val getVotesForRound = GetVotesForRound(jdbi)
    private val countVotesForRound = CountVotesForRound(jdbi)

    override fun createLadder(param: CreateLadderParam): Result<Ladder, AppError> =
        createLadder.execute(param)

    override fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError> =
        getLadderById.execute(param)

    override fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError> =
        getLadderList.execute(param)

    override fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError> =
        addLadderActivity.execute(param)

    override fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError> =
        removeLadderActivity.execute(param)

    override fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> =
        getLadderActivities.execute(param)

    override fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError> =
        updateLadderStatus.execute(param)

    override fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError> =
        setLadderWinner.execute(param)

    override fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError> =
        restartLadder.execute(param)

    override fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> =
        updateActivityLossAndBracket.execute(param)

    override fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> =
        bulkInsertParticipants.execute(param)

    override fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> =
        getParticipants.execute(param)

    override fun castVote(param: CastVoteParam): Result<LadderVote, AppError> =
        castVote.execute(param)

    override fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> =
        getVotesForRound.execute(param)

    override fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError> =
        countVotesForRound.execute(param)

    override fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError> =
        jdbi.inTransaction<Result<T, AppError>, Exception> { handle ->
            val locked = handle.createQuery("SELECT id FROM activity_ladders WHERE id = :id FOR UPDATE")
                .bind("id", ladderId)
                .mapTo(UUID::class.java)
                .findOne()
            if (locked.isEmpty) {
                failure(NotFoundError("Ladder", ladderId.toString()))
            } else {
                val scoped = HandleScopedActivityLadderClient(handle)
                block(scoped)
            }
        }
}

/**
 * A scoped [ActivityLadderClient] that executes all operations on the provided [Handle].
 * Used exclusively within [JdbiActivityLadderClient.withLadderLocked] to ensure all operations
 * in the locked block run on the same transactional handle that holds the SELECT FOR UPDATE lock.
 *
 * Validators are applied identically to [JdbiActivityLadderClient] — every method runs its paired
 * validator before touching the database.
 */
internal class HandleScopedActivityLadderClient(private val handle: Handle) : ActivityLadderClient {

    private val validateCreateLadder = com.acme.clients.activityladderclient.internal.validations.ValidateCreateLadder()
    private val validateGetLadderById = com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderById()
    private val validateGetLadderList = com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderList()
    private val validateAddLadderActivity = com.acme.clients.activityladderclient.internal.validations.ValidateAddLadderActivity()
    private val validateRemoveLadderActivity = com.acme.clients.activityladderclient.internal.validations.ValidateRemoveLadderActivity()
    private val validateGetLadderActivities = com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderActivities()
    private val validateUpdateLadderState = com.acme.clients.activityladderclient.internal.validations.ValidateUpdateLadderState()
    private val validateSetLadderWinner = com.acme.clients.activityladderclient.internal.validations.ValidateSetLadderWinner()
    private val validateRestartLadder = com.acme.clients.activityladderclient.internal.validations.ValidateRestartLadder()
    private val validateUpdateActivityLossAndBracket = com.acme.clients.activityladderclient.internal.validations.ValidateUpdateActivityLossAndBracket()
    private val validateBulkInsertParticipants = com.acme.clients.activityladderclient.internal.validations.ValidateBulkInsertParticipants()
    private val validateGetParticipants = com.acme.clients.activityladderclient.internal.validations.ValidateGetParticipants()
    private val validateCastVote = com.acme.clients.activityladderclient.internal.validations.ValidateCastVote()
    private val validateGetVotesForRound = com.acme.clients.activityladderclient.internal.validations.ValidateGetVotesForRound()
    private val validateCountVotesForRound = com.acme.clients.activityladderclient.internal.validations.ValidateCountVotesForRound()

    override fun createLadder(param: CreateLadderParam): Result<Ladder, AppError> =
        validateCreateLadder.execute(param).flatMap {
            com.acme.clients.common.success(CreateLadder.insertLadder(handle, param))
        }

    override fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError> =
        validateGetLadderById.execute(param).flatMap {
            val ladder = GetLadderById.findById(handle, param)
            if (ladder != null) com.acme.clients.common.success(ladder) else failure(NotFoundError("Ladder", param.id.toString()))
        }

    override fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError> =
        validateGetLadderList.execute(param).flatMap {
            com.acme.clients.common.success(GetLadderList.fetchList(handle, param))
        }

    override fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError> =
        validateAddLadderActivity.execute(param).flatMap {
            com.acme.clients.common.success(AddLadderActivity.insert(handle, param))
        }

    override fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError> =
        validateRemoveLadderActivity.execute(param).flatMap {
            val rowsAffected = RemoveLadderActivity.delete(handle, param)
            if (rowsAffected > 0) com.acme.clients.common.success(Unit) else failure(NotFoundError("LadderActivity", param.activityId.toString()))
        }

    override fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> =
        validateGetLadderActivities.execute(param).flatMap {
            com.acme.clients.common.success(GetLadderActivities.fetchByLadderId(handle, param))
        }

    override fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError> =
        validateUpdateLadderState.execute(param).flatMap {
            val ladder = UpdateLadderStatus.update(handle, param)
            if (ladder != null) com.acme.clients.common.success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
        }

    override fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError> =
        validateSetLadderWinner.execute(param).flatMap {
            val ladder = SetLadderWinner.setWinner(handle, param)
            if (ladder != null) com.acme.clients.common.success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
        }

    override fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError> =
        validateRestartLadder.execute(param).flatMap {
            val ladder = RestartLadder.restart(handle, param)
            if (ladder != null) com.acme.clients.common.success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
        }

    override fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> =
        validateUpdateActivityLossAndBracket.execute(param).flatMap {
            val activity = UpdateActivityLossAndBracket.update(handle, param)
            if (activity != null) com.acme.clients.common.success(activity) else failure(NotFoundError("LadderActivity", param.activityId.toString()))
        }

    override fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> =
        validateBulkInsertParticipants.execute(param).flatMap {
            com.acme.clients.common.success(BulkInsertParticipants.bulkInsert(handle, param))
        }

    override fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> =
        validateGetParticipants.execute(param).flatMap {
            com.acme.clients.common.success(GetParticipants.fetchByLadderId(handle, param))
        }

    override fun castVote(param: CastVoteParam): Result<LadderVote, AppError> =
        validateCastVote.execute(param).flatMap {
            try {
                val vote = CastVote.insert(handle, param)
                com.acme.clients.common.success(vote)
            } catch (e: Exception) {
                if (e.message?.contains("uq_ladder_votes_round_user") == true || e.message?.contains("duplicate key") == true) {
                    failure(com.acme.clients.common.error.ConflictError("LadderVote", "User ${param.userId} already voted in round ${param.roundNumber}"))
                } else {
                    throw e
                }
            }
        }

    override fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> =
        validateGetVotesForRound.execute(param).flatMap {
            com.acme.clients.common.success(GetVotesForRound.fetchForRound(handle, param))
        }

    override fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError> =
        validateCountVotesForRound.execute(param).flatMap {
            com.acme.clients.common.success(CountVotesForRound.countForRound(handle, param))
        }

    override fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError> {
        // Already within a locked transaction — just run the block with this scoped client.
        // The caller is responsible for not nesting withLadderLocked calls on the same ladder.
        return block(this)
    }
}
