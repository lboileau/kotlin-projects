package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.internal.adapters.LadderRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderById
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetLadderById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetLadderById::class.java)
    private val validate = ValidateGetLadderById()

    fun execute(param: GetLadderByIdParam): Result<Ladder, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding ladder by id={}", param.id)
        val ladder = jdbi.withHandle<Ladder?, Exception> { handle ->
            findById(handle, param)
        }
        return if (ladder != null) success(ladder) else failure(NotFoundError("Ladder", param.id.toString()))
    }

    companion object {
        fun findById(handle: Handle, param: GetLadderByIdParam): Ladder? =
            handle.createQuery(
                """
                SELECT id, creator_id, title, status, current_round_number,
                    current_match_activity_a_id, current_match_activity_b_id, is_final_round,
                    is_grand_final_reset, winner_activity_id, created_at, updated_at
                FROM activity_ladders WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> LadderRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
    }
}
