package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.CastVoteParam
import com.acme.clients.activityladderclient.internal.adapters.LadderVoteRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateCastVote
import com.acme.clients.activityladderclient.model.LadderVote
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CastVote(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CastVote::class.java)
    private val validate = ValidateCastVote()

    fun execute(param: CastVoteParam): Result<LadderVote, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Casting vote ladderId={} round={} userId={}", param.ladderId, param.roundNumber, param.userId)
        return try {
            val vote = jdbi.withHandle<LadderVote, Exception> { handle ->
                insert(handle, param)
            }
            success(vote)
        } catch (e: Exception) {
            if (e.message?.contains("uq_ladder_votes_round_user") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("LadderVote", "User ${param.userId} already voted in round ${param.roundNumber}"))
            } else {
                throw e
            }
        }
    }

    companion object {
        fun insert(handle: Handle, param: CastVoteParam): LadderVote {
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO ladder_votes (id, ladder_id, round_number, user_id, voted_for_activity_id, created_at)
                VALUES (:id, :ladderId, :roundNumber, :userId, :votedForActivityId, :createdAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("ladderId", param.ladderId)
                .bind("roundNumber", param.roundNumber)
                .bind("userId", param.userId)
                .bind("votedForActivityId", param.votedForActivityId)
                .bind("createdAt", now)
                .execute()

            return handle.createQuery(
                """
                SELECT id, ladder_id, round_number, user_id, voted_for_activity_id, created_at
                FROM ladder_votes WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> LadderVoteRowAdapter.fromResultSet(rs) }
                .one()
        }
    }
}
