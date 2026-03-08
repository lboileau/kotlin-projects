package com.acme.clients.invitationclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.GetByPlanIdAndUserIdParam
import com.acme.clients.invitationclient.internal.adapters.InvitationRowAdapter
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetInvitationByPlanIdAndUserId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetInvitationByPlanIdAndUserId::class.java)

    fun execute(param: GetByPlanIdAndUserIdParam): Result<Invitation?, AppError> {
        logger.debug("Getting invitation for planId={} userId={}", param.planId, param.userId)
        val invitation = jdbi.withHandle<Invitation?, Exception> { handle ->
            handle.createQuery("SELECT * FROM invitations WHERE plan_id = :planId AND user_id = :userId")
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .map { rs, _ -> InvitationRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return success(invitation)
    }
}
