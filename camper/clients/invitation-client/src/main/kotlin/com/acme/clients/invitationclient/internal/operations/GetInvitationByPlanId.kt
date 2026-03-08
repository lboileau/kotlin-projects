package com.acme.clients.invitationclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.GetByPlanIdParam
import com.acme.clients.invitationclient.internal.adapters.InvitationRowAdapter
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetInvitationByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetInvitationByPlanId::class.java)

    fun execute(param: GetByPlanIdParam): Result<List<Invitation>, AppError> {
        logger.debug("Getting invitations for planId={}", param.planId)
        val invitations = jdbi.withHandle<List<Invitation>, Exception> { handle ->
            handle.createQuery("SELECT * FROM invitations WHERE plan_id = :planId ORDER BY sent_at DESC")
                .bind("planId", param.planId)
                .map { rs, _ -> InvitationRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(invitations)
    }
}
