package com.acme.services.camperservice.features.auth.error

import com.acme.clients.common.error.AppError

sealed class AuthError(override val message: String) : AppError {
    data class InvalidOtp(val email: String) : AuthError("Invalid or expired code")
    data class OtpMaxAttempts(val email: String) : AuthError("Too many failed attempts")
    data class InvalidRefreshToken(val reason: String = "Invalid or expired refresh token") : AuthError(reason)
    data class InvalidRequest(val field: String, val reason: String) : AuthError("Invalid $field: $reason")
    data class OtpRateLimited(val email: String) : AuthError("Too many OTP requests")
}
