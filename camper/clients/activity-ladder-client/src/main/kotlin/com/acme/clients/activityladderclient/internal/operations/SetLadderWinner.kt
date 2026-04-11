package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateSetLadderWinner
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class SetLadderWinner(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(SetLadderWinner::class.java)
    private val validate = ValidateSetLadderWinner()

    fun execute(param: SetLadderWinnerParam): Result<Ladder, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Setting winner for ladder id={} winnerActivityId={}", param.ladderId, param.winnerActivityId)
        val ladder = jdbi.withHandle<Ladder?, Exception> { handle ->
            setWinner(handle, param)
        }
        return if (ladder != null) success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
    }

    companion object {
        fun setWinner(handle: Handle, param: SetLadderWinnerParam): Ladder? {
            val now = Instant.now()
            val rowsAffected = handle.createUpdate(
                """
                UPDATE activity_ladders
                SET status = 'COMPLETED',
                    winner_activity_id = CAST(:winnerActivityId AS uuid),
                    updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.ladderId)
                .bind("winnerActivityId", param.winnerActivityId.toString())
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
