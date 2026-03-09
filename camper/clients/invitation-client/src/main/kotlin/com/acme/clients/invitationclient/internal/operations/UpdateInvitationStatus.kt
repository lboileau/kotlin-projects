package com.acme.clients.invitationclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.UpdateStatusParam
import com.acme.clients.invitationclient.internal.adapters.InvitationRowAdapter
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateInvitationStatus(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateInvitationStatus::class.java)

    fun execute(param: UpdateStatusParam): Result<Invitation, AppError> {
        logger.debug("Updating invitation id={} status={}", param.id, param.status)
        val now = Instant.now()

        val invitation = jdbi.withHandle<Invitation?, Exception> { handle ->
            val query = if (param.resendEmailId != null) {
                handle.createQuery(
                    """
                    UPDATE invitations SET status = :status, resend_email_id = :resendEmailId, updated_at = :updatedAt
                    WHERE id = :id
                    RETURNING *
                    """.trimIndent()
                ).bind("resendEmailId", param.resendEmailId)
            } else {
                handle.createQuery(
                    """
                    UPDATE invitations SET status = :status, updated_at = :updatedAt
                    WHERE id = :id
                    RETURNING *
                    """.trimIndent()
                )
            }
            query
                .bind("id", param.id)
                .bind("status", param.status)
                .bind("updatedAt", now)
                .map { rs, _ -> InvitationRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }

        return if (invitation != null) {
            success(invitation)
        } else {
            failure(NotFoundError("Invitation", param.id.toString()))
        }
    }
}
