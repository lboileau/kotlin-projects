package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateLadderState
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateLadderStatus(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateLadderStatus::class.java)
    private val validate = ValidateUpdateLadderState()

    fun execute(param: UpdateLadderStateParam): Result<Ladder, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating ladder state id={} status={}", param.ladderId, param.status)
        val ladder = jdbi.withHandle<Ladder?, Exception> { handle ->
            update(handle, param)
        }
        return if (ladder != null) success(ladder) else failure(NotFoundError("Ladder", param.ladderId.toString()))
    }

    companion object {
        fun update(handle: Handle, param: UpdateLadderStateParam): Ladder? {
            val now = Instant.now()
            val rowsAffected = handle.createUpdate(
                """
                UPDATE activity_ladders
                SET status = :status,
                    current_round_number = :currentRoundNumber,
                    current_match_activity_a_id = CAST(:currentMatchActivityAId AS uuid),
                    current_match_activity_b_id = CAST(:currentMatchActivityBId AS uuid),
                    is_final_round = :isFinalRound,
                    is_grand_final_reset = :isGrandFinalReset,
                    updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.ladderId)
                .bind("status", param.status.name)
                .bind("currentRoundNumber", param.currentRoundNumber)
                .bind("currentMatchActivityAId", param.currentMatchActivityAId?.toString())
                .bind("currentMatchActivityBId", param.currentMatchActivityBId?.toString())
                .bind("isFinalRound", param.isFinalRound)
                .bind("isGrandFinalReset", param.isGrandFinalReset)
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
