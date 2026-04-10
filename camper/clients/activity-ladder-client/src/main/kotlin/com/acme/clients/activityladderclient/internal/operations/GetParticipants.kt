package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.internal.adapters.LadderParticipantRowAdapter
import com.acme.clients.activityladderclient.internal.validations.ValidateGetParticipants
import com.acme.clients.activityladderclient.model.LadderParticipant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetParticipants(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetParticipants::class.java)
    private val validate = ValidateGetParticipants()

    fun execute(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching participants for ladder id={}", param.ladderId)
        val participants = jdbi.withHandle<List<LadderParticipant>, Exception> { handle ->
            fetchByLadderId(handle, param)
        }
        return success(participants)
    }

    companion object {
        fun fetchByLadderId(handle: Handle, param: GetParticipantsParam): List<LadderParticipant> =
            handle.createQuery(
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
