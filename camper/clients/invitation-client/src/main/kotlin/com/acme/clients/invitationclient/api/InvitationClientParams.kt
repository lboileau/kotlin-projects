package com.acme.clients.invitationclient.api

import java.util.UUID

data class UpsertInvitationParam(
    val planId: UUID,
    val userId: UUID,
    val email: String,
    val inviterId: UUID,
    val resendEmailId: String?,
    val status: String
)

data class GetByPlanIdParam(val planId: UUID)

data class GetByPlanIdAndUserIdParam(val planId: UUID, val userId: UUID)

data class GetByResendEmailIdParam(val resendEmailId: String)

data class UpdateStatusParam(
    val id: UUID,
    val status: String,
    val resendEmailId: String? = null
)
