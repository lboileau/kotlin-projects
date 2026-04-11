package com.acme.clients.activityladderclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.model.Ladder
import com.acme.clients.activityladderclient.model.LadderActivity
import com.acme.clients.activityladderclient.model.LadderParticipant
import com.acme.clients.activityladderclient.model.LadderVote
import java.util.UUID

/**
 * Client interface for activity ladder CRUD and state-machine operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface ActivityLadderClient {

    // Ladder CRUD

    /** Create a new ladder in DRAFT status. */
    fun createLadder(param: CreateLadderParam): Result<Ladder, AppError>

    /** Fetch a ladder by id. Returns [com.acme.clients.common.error.NotFoundError] if not found. */
    fun getLadderById(param: GetLadderByIdParam): Result<Ladder, AppError>

    /** List all ladders, newest first. Caller-side pagination via limit/offset. */
    fun getLadderList(param: GetLadderListParam): Result<List<Ladder>, AppError>

    // Activity CRUD (within a ladder)

    /** Add an activity to a ladder; assigns display_order = MAX+1 atomically. */
    fun addActivity(param: AddLadderActivityParam): Result<LadderActivity, AppError>

    /** Delete an activity from a ladder. */
    fun removeActivity(param: RemoveLadderActivityParam): Result<Unit, AppError>

    /** List all activities for a ladder, ordered by display_order. */
    fun getActivities(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError>

    // Ladder state mutation (used by round-resolution logic)

    /** Update status, currentRoundNumber, match, isFinalRound, isGrandFinalReset fields. */
    fun updateLadderState(param: UpdateLadderStateParam): Result<Ladder, AppError>

    /** Set the winner and mark COMPLETED in a single statement. */
    fun setLadderWinner(param: SetLadderWinnerParam): Result<Ladder, AppError>

    /**
     * Runs the entire restart operation inside a single transaction:
     * delete votes + participants, reset all activities to 0 losses / WINNERS bracket,
     * reset ladder state to DRAFT. Returns the updated ladder.
     */
    fun restartLadder(param: RestartLadderParam): Result<Ladder, AppError>

    // Activity state (driven by round resolution)

    /** Increment losses and set bracket on a single activity. */
    fun updateActivityLossAndBracket(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError>

    // Participants

    /**
     * Idempotent bulk insert of participants at Start time.
     * Uses INSERT ... ON CONFLICT (ladder_id, user_id) DO NOTHING so accidental re-runs
     * never corrupt an existing frozen voter set.
     */
    fun bulkInsertParticipants(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError>

    /** List participants for a ladder. */
    fun getParticipants(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError>

    // Votes

    /**
     * Cast a single vote. This method MUST be invoked by the caller inside a
     * transaction that has previously issued SELECT ... FOR UPDATE on the
     * activity_ladders row for this ladder. The implementation therefore performs
     * no locking of its own — it relies on the caller's transaction.
     *
     * Returns [com.acme.clients.common.error.ConflictError] on duplicate (ladder_id, round_number, user_id) violation.
     */
    fun castVote(param: CastVoteParam): Result<LadderVote, AppError>

    /** Fetch votes for a round (caller inside txn; used post-vote to tally). */
    fun getVotesForRound(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError>

    /** Count votes for a round. Cheap check used before tallying. */
    fun countVotesForRound(param: CountVotesForRoundParam): Result<Int, AppError>

    /**
     * Runs [block] inside a single JDBI transaction that has acquired
     * `SELECT id FROM activity_ladders WHERE id = :id FOR UPDATE` on the target row.
     * The service's vote-resolution action is the sole caller.
     *
     * The [block] receives a scoped [ActivityLadderClient] instance whose operations
     * all share the transactional handle; any operation called on this scoped client
     * is part of the same transaction that holds the lock on the ladder row.
     */
    fun <T> withLadderLocked(ladderId: UUID, block: (ActivityLadderClient) -> Result<T, AppError>): Result<T, AppError>
}
