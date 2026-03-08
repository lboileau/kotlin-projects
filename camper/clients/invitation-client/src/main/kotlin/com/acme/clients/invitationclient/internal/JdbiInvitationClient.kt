package com.acme.clients.invitationclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.invitationclient.api.*
import com.acme.clients.invitationclient.internal.operations.*
import com.acme.clients.invitationclient.model.Invitation
import org.jdbi.v3.core.Jdbi

internal class JdbiInvitationClient(jdbi: Jdbi) : InvitationClient {
    private val upsertInvitation = UpsertInvitation(jdbi)
    private val getByPlanId = GetInvitationByPlanId(jdbi)
    private val getByPlanIdAndUserId = GetInvitationByPlanIdAndUserId(jdbi)
    private val getByResendEmailId = GetInvitationByResendEmailId(jdbi)
    private val updateStatus = UpdateInvitationStatus(jdbi)

    override fun upsert(param: UpsertInvitationParam): Result<Invitation, AppError> = upsertInvitation.execute(param)
    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Invitation>, AppError> = getByPlanId.execute(param)
    override fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<Invitation?, AppError> = getByPlanIdAndUserId.execute(param)
    override fun getByResendEmailId(param: GetByResendEmailIdParam): Result<Invitation?, AppError> = getByResendEmailId.execute(param)
    override fun updateStatus(param: UpdateStatusParam): Result<Invitation, AppError> = updateStatus.execute(param)
}
