package com.acme.services.camperservice.features.auth.dto

import com.acme.services.camperservice.features.user.dto.AvatarResponse
import java.util.UUID

data class OtpRequestRequest(val email: String)
data class OtpRequestResponse(val message: String = "OTP sent")

data class OtpVerifyRequest(val email: String, val code: String)
data class OtpVerifyResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserResponse
)
data class AuthUserResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val avatarSeed: String?,
    val profileCompleted: Boolean,
    val avatar: AvatarResponse?
)

data class TokenRefreshRequest(val refreshToken: String)
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

data class LogoutRequest(val refreshToken: String)
