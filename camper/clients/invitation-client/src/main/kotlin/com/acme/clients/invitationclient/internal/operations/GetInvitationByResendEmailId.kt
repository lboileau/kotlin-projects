package com.acme.clients.invitationclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.GetByResendEmailIdParam
import com.acme.clients.invitationclient.internal.adapters.InvitationRowAdapter
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetInvitationByResendEmailId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetInvitationByResendEmailId::class.java)

    fun execute(param: GetByResendEmailIdParam): Result<Invitation?, AppError> {
        logger.debug("Getting invitation for resendEmailId={}", param.resendEmailId)
        val invitation = jdbi.withHandle<Invitation?, Exception> { handle ->
            handle.createQuery("SELECT * FROM invitations WHERE resend_email_id = :resendEmailId")
                .bind("resendEmailId", param.resendEmailId)
                .map { rs, _ -> InvitationRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return success(invitation)
    }
}
