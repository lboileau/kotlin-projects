package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.CreateLadderParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateCreateLadder
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateLadder(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateLadder::class.java)
    private val validate = ValidateCreateLadder()

    fun execute(param: CreateLadderParam): Result<Ladder, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating ladder title={}", param.title)
        val ladder = jdbi.withHandle<Ladder, Exception> { handle ->
            insertLadder(handle, param)
        }
        return success(ladder)
    }

    companion object {
        fun insertLadder(handle: Handle, param: CreateLadderParam): Ladder {
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO activity_ladders (id, creator_id, title, status, current_round_number,
                    current_match_activity_a_id, current_match_activity_b_id, is_final_round,
                    is_grand_final_reset, winner_activity_id, created_at, updated_at)
                VALUES (:id, :creatorId, :title, 'DRAFT', NULL, NULL, NULL, false, false, NULL, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("creatorId", param.creatorId)
                .bind("title", param.title)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            return handle.createQuery(
                """
                SELECT id, creator_id, title, status, current_round_number,
                    current_match_activity_a_id, current_match_activity_b_id, is_final_round,
                    is_grand_final_reset, winner_activity_id, created_at, updated_at
                FROM activity_ladders WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> LadderRowAdapter.fromResultSet(rs) }
                .one()
        }
    }
}
