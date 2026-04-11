package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.BulkInsertParticipantsParam
import com.acme.clients.activityladderclient.internal.adapters.LadderParticipantRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateBulkInsertParticipants
import com.acme.clients.activityladderclient.model.LadderParticipant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class BulkInsertParticipants(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(BulkInsertParticipants::class.java)
    private val validate = ValidateBulkInsertParticipants()

    fun execute(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Bulk inserting {} participants for ladder id={}", param.userIds.size, param.ladderId)
        val participants = jdbi.withHandle<List<LadderParticipant>, Exception> { handle ->
            bulkInsert(handle, param)
        }
        return success(participants)
    }

    companion object {
        fun bulkInsert(handle: Handle, param: BulkInsertParticipantsParam): List<LadderParticipant> {
            if (param.userIds.isEmpty()) return emptyList()

            val now = Instant.now()
            val batch = handle.prepareBatch(
                """
                INSERT INTO ladder_participants (id, ladder_id, user_id, created_at)
                VALUES (:id, :ladderId, :userId, :createdAt)
                ON CONFLICT (ladder_id, user_id) DO NOTHING
                """.trimIndent()
            )
            for (userId in param.userIds) {
                batch
                    .bind("id", UUID.randomUUID())
                    .bind("ladderId", param.ladderId)
                    .bind("userId", userId)
                    .bind("createdAt", now)
                    .add()
            }
            batch.execute()

            return handle.createQuery(
                """
                SELECT id, ladder_id, user_id, created_at
                FROM ladder_participants
                WHERE ladder_id = :ladderId
                """.trimIndent()
            )
                .bind("ladderId", param.ladderId)
                .map { rs, _ -> LadderParticipantRowAdapter.fromResultSet(rs) }
                .list()
        }
    }
}
