package com.acme.clients.invitationclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.UpsertInvitationParam
import com.acme.clients.invitationclient.internal.adapters.InvitationRowAdapter
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class UpsertInvitation(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpsertInvitation::class.java)

    fun execute(param: UpsertInvitationParam): Result<Invitation, AppError> {
        logger.debug("Upserting invitation planId={} userId={}", param.planId, param.userId)
        val id = UUID.randomUUID()
        val now = Instant.now()

        val invitation = jdbi.withHandle<Invitation, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO invitations (id, plan_id, user_id, email, inviter_id, resend_email_id, status, sent_at, updated_at)
                VALUES (:id, :planId, :userId, :email, :inviterId, :resendEmailId, :status, :sentAt, :updatedAt)
                ON CONFLICT (plan_id, user_id)
                DO UPDATE SET
                    email = EXCLUDED.email,
                    inviter_id = EXCLUDED.inviter_id,
                    resend_email_id = EXCLUDED.resend_email_id,
                    status = EXCLUDED.status,
                    sent_at = EXCLUDED.sent_at,
                    updated_at = EXCLUDED.updated_at
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .bind("email", param.email)
                .bind("inviterId", param.inviterId)
                .bind("resendEmailId", param.resendEmailId)
                .bind("status", param.status)
                .bind("sentAt", now)
                .bind("updatedAt", now)
                .map { rs, _ -> InvitationRowAdapter.fromResultSet(rs) }
                .one()
        }

        return success(invitation)
    }
}
