package com.acme.services.camperservice.features.user.dto

import java.time.Instant
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val experienceLevel: String?,
    val avatarSeed: String?,
    val profileCompleted: Boolean,
    val dietaryRestrictions: List<String>,
    val avatar: AvatarResponse?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AuthResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val avatarSeed: String?,
    val profileCompleted: Boolean,
    val avatar: AvatarResponse?
)

data class AvatarResponse(
    val hairStyle: String,
    val hairColor: String,
    val skinColor: String,
    val clothingStyle: String,
    val pantsColor: String,
    val shirtColor: String
)
