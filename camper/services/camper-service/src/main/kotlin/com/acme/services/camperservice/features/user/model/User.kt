package com.acme.services.camperservice.features.user.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
