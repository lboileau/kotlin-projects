package com.acme.clients.userclient.api

import java.util.UUID

/** Parameter for retrieving a user by ID. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving a user by email. */
data class GetByEmailParam(val email: String)

/** Parameter for creating a new user. */
data class CreateUserParam(val email: String, val username: String? = null)

/** Parameter for getting or creating a user by email. Username is optional (derived from email if absent). */
data class GetOrCreateUserParam(val email: String, val username: String? = null)

/** Parameter for updating a user's username. */
data class UpdateUserParam(val id: UUID, val username: String)
