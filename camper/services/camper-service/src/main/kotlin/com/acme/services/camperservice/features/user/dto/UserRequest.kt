package com.acme.services.camperservice.features.user.dto

data class CreateUserRequest(val email: String, val username: String? = null)

data class AuthRequest(val email: String)

data class UpdateUserRequest(
    val username: String,
    val experienceLevel: String? = null,
    val dietaryRestrictions: List<String>? = null,
    val profileCompleted: Boolean? = null,
    val avatarSeed: String? = null
)
