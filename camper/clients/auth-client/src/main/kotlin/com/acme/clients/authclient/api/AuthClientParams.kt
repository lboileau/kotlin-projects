package com.acme.clients.authclient.api

import java.time.Instant
import java.util.UUID

/** Parameter for creating a new OTP code record. */
data class CreateOtpParam(val email: String, val codeHash: String, val expiresAt: Instant)

/** Parameter for finding the most recent valid OTP for an email. */
data class FindLatestValidOtpParam(val email: String)

/** Parameter for incrementing the attempt count on an OTP record. */
data class IncrementOtpAttemptCountParam(val id: UUID)

/** Parameter for marking an OTP as used. */
data class MarkOtpUsedParam(val id: UUID)

/** Parameter for creating a new refresh token record. */
data class CreateRefreshTokenParam(val userId: UUID, val familyId: UUID, val tokenHash: String, val expiresAt: Instant)

/** Parameter for finding a valid refresh token by hash. */
data class FindValidRefreshTokenParam(val tokenHash: String)

/** Parameter for revoking a single refresh token by ID. */
data class RevokeRefreshTokenParam(val id: UUID)

/** Parameter for revoking all refresh tokens for a user. */
data class RevokeAllUserRefreshTokensParam(val userId: UUID)

/** Parameter for revoking all refresh tokens in a token family. */
data class RevokeTokenFamilyParam(val familyId: UUID)
