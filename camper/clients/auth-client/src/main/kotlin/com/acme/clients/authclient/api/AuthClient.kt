package com.acme.clients.authclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.authclient.model.AuthOtpCode
import com.acme.clients.authclient.model.AuthRefreshToken

/**
 * Client interface for authentication operations (OTP codes and refresh tokens).
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface AuthClient {
    /** Store a new OTP code hash for the given email. */
    fun createOtp(param: CreateOtpParam): Result<AuthOtpCode, AppError>

    /** Find the most recent valid (unexpired, unused, under max attempts) OTP for an email. */
    fun findLatestValidOtp(param: FindLatestValidOtpParam): Result<AuthOtpCode?, AppError>

    /** Increment the attempt_count on an OTP record (called on failed verify). */
    fun incrementOtpAttemptCount(param: IncrementOtpAttemptCountParam): Result<Unit, AppError>

    /** Mark an OTP as used (set used_at). */
    fun markOtpUsed(param: MarkOtpUsedParam): Result<Unit, AppError>

    /** Store a new refresh token hash with family_id. */
    fun createRefreshToken(param: CreateRefreshTokenParam): Result<AuthRefreshToken, AppError>

    /** Find a valid (unexpired, unrevoked) refresh token by hash. */
    fun findValidRefreshToken(param: FindValidRefreshTokenParam): Result<AuthRefreshToken?, AppError>

    /** Revoke a single refresh token by ID. */
    fun revokeRefreshToken(param: RevokeRefreshTokenParam): Result<Unit, AppError>

    /** Revoke all refresh tokens for a user. */
    fun revokeAllUserRefreshTokens(param: RevokeAllUserRefreshTokensParam): Result<Unit, AppError>

    /** Revoke all refresh tokens in a token family (reuse detection). */
    fun revokeTokenFamily(param: RevokeTokenFamilyParam): Result<Unit, AppError>
}
