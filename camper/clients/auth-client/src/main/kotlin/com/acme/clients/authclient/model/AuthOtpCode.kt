package com.acme.clients.authclient.model

import java.time.Instant
import java.util.UUID

data class AuthOtpCode(
    val id: UUID,
    val email: String,
    val codeHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
    val attemptCount: Int,
    val createdAt: Instant
)
