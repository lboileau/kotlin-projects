package com.acme.clients.authclient.model

import java.time.Instant
import java.util.UUID

data class AuthRefreshToken(
    val id: UUID,
    val userId: UUID,
    val familyId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant
)
