package com.acme.clients.invitationclient.model

import java.time.Instant
import java.util.UUID

data class Invitation(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val email: String,
    val inviterId: UUID,
    val resendEmailId: String?,
    val status: String,
    val sentAt: Instant,
    val updatedAt: Instant
)
