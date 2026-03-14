package com.acme.clients.userclient.api

import java.util.UUID

/** Parameter for retrieving a user by ID. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving a user by email. */
data class GetByEmailParam(val email: String)

/** Parameter for creating a new user. */
data class CreateUserParam(val email: String, val username: String? = null, val avatarSeed: String? = null)

/** Parameter for getting or creating a user by email. Username is optional (derived from email if absent). */
data class GetOrCreateUserParam(val email: String, val username: String? = null)

/** Parameter for updating a user's profile. Null fields mean "no change" (except username which is always required). */
data class UpdateUserParam(
    val id: UUID,
    val username: String,
    val experienceLevel: String? = null,
    val avatarSeed: String? = null,
    val profileCompleted: Boolean? = null,
    val dietaryRestrictions: List<String>? = null
)

/** Parameter for retrieving dietary restrictions for a user. */
data class GetDietaryRestrictionsParam(val userId: UUID)

/** Parameter for replacing all dietary restrictions for a user. */
data class SetDietaryRestrictionsParam(val userId: UUID, val restrictions: List<String>)
