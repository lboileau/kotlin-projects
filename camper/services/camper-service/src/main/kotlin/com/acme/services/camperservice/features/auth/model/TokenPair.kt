package com.acme.services.camperservice.features.auth.model

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
