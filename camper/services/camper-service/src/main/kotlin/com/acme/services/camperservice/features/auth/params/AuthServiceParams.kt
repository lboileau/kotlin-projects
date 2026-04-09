package com.acme.services.camperservice.features.auth.params

import java.util.UUID

/** Parameter for requesting an OTP code to be sent to the given email. */
data class RequestOtpParam(val email: String, val clientIp: String, val clientId: UUID?)

/** Parameter for verifying an OTP code and obtaining tokens. */
data class VerifyOtpParam(val email: String, val code: String)

/** Parameter for refreshing an access token using a refresh token. */
data class RefreshTokenParam(val refreshToken: String)

/** Parameter for logging out by revoking a refresh token. */
data class LogoutParam(val refreshToken: String)
