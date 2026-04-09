package com.acme.services.camperservice.features.auth.model

import com.acme.services.camperservice.features.user.model.User

data class OtpVerifyResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)
