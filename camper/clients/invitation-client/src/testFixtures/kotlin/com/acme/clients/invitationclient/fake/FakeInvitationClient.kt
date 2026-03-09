package com.acme.clients.invitationclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.invitationclient.api.GetByPlanIdAndUserIdParam
import com.acme.clients.invitationclient.api.GetByPlanIdParam
import com.acme.clients.invitationclient.api.GetByResendEmailIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.api.UpdateStatusParam
import com.acme.clients.invitationclient.api.UpsertInvitationParam
import com.acme.clients.invitationclient.model.Invitation
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeInvitationClient : InvitationClient {
    private val store = ConcurrentHashMap<UUID, Invitation>()

    override fun upsert(param: UpsertInvitationParam): Result<Invitation, AppError> {
        val existing = store.values.find { it.planId == param.planId && it.userId == param.userId }
        val now = Instant.now()
        val invitation = if (existing != null) {
            existing.copy(
                email = param.email,
                inviterId = param.inviterId,
                resendEmailId = param.resendEmailId,
                status = param.status,
                updatedAt = now
            )
        } else {
            Invitation(
                id = UUID.randomUUID(),
                planId = param.planId,
                userId = param.userId,
                email = param.email,
                inviterId = param.inviterId,
                resendEmailId = param.resendEmailId,
                status = param.status,
                sentAt = now,
                updatedAt = now
            )
        }
        store[invitation.id] = invitation
        return success(invitation)
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Invitation>, AppError> {
        val invitations = store.values.filter { it.planId == param.planId }
        return success(invitations)
    }

    override fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<Invitation?, AppError> {
        val invitation = store.values.find { it.planId == param.planId && it.userId == param.userId }
        return success(invitation)
    }

    override fun getByResendEmailId(param: GetByResendEmailIdParam): Result<Invitation?, AppError> {
        val invitation = store.values.find { it.resendEmailId == param.resendEmailId }
        return success(invitation)
    }

    override fun updateStatus(param: UpdateStatusParam): Result<Invitation, AppError> {
        val existing = store[param.id]
            ?: return failure(NotFoundError("Invitation", param.id.toString()))
        val updated = existing.copy(
            status = param.status,
            resendEmailId = param.resendEmailId ?: existing.resendEmailId,
            updatedAt = Instant.now()
        )
        store[updated.id] = updated
        return success(updated)
    }

    fun reset() = store.clear()

    fun seed(vararg entities: Invitation) {
        entities.forEach { store[it.id] = it }
    }
}
