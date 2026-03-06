package com.acme.services.camperservice.features.user.dto

import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AuthResponse(
    val id: UUID,
    val email: String,
    val username: String?
)
