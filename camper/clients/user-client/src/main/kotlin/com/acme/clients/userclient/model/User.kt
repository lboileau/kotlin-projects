package com.acme.clients.userclient.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val experienceLevel: String? = null,
    val avatarSeed: String? = null,
    val profileCompleted: Boolean = false,
    val dietaryRestrictions: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)
