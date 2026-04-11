package com.acme.clients.activityladderclient.internal

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

    override fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
