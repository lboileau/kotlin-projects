package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.RestartLadderParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateRestartLadder
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class RestartLadder(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RestartLadder::class.java)
    private val validate = ValidateRestartLadder()

    fun execute(param: RestartLadderParam): Result<Ladder, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Restarting ladder id={}", param.ladderId)
        val ladder = jdbi.inTransaction<Ladder?, Exception> { handle ->
            restart(handle, param)
        }
        return if (ladder != null) success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
    }

    companion object {
        fun restart(handle: Handle, param: RestartLadderParam): Ladder? {
            // Delete all votes for this ladder
            handle.createUpdate("DELETE FROM ladder_votes WHERE ladder_id = :ladderId")
                .bind("ladderId", param.ladderId)
                .execute()

            // Delete all participants for this ladder
            handle.createUpdate("DELETE FROM ladder_participants WHERE ladder_id = :ladderId")
                .bind("ladderId", param.ladderId)
                .execute()

            // Reset all activities to 0 losses and WINNERS bracket
            handle.createUpdate(
                """
                UPDATE ladder_activities
                SET losses = 0, bracket = 'WINNERS'
                WHERE ladder_id = :ladderId
                """.trimIndent()
            )
                .bind("ladderId", param.ladderId)
                .execute()

            // Reset ladder state to DRAFT
            val now = Instant.now()
            val rowsAffected = handle.createUpdate(
                """
                UPDATE activity_ladders
                SET status = 'DRAFT',
                    current_round_number = NULL,
                    current_match_activity_a_id = NULL,
                    current_match_activity_b_id = NULL,
                    is_final_round = false,
                    is_grand_final_reset = false,
                    winner_activity_id = NULL,
                    updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.ladderId)
                .bind("updatedAt", now)
                .execute()

            if (rowsAffected == 0) return null

            return handle.createQuery(
                """
                SELECT id, creator_id, title, status, current_round_number,
                    current_match_activity_a_id, current_match_activity_b_id, is_final_round,
                    is_grand_final_reset, winner_activity_id, created_at, updated_at
                FROM activity_ladders WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.ladderId)
                .map { rs, _ -> LadderRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
    }
}
