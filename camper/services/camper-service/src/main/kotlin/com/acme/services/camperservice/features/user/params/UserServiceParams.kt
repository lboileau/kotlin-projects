package com.acme.services.camperservice.features.user.params

import java.util.UUID

data class GetUserByIdParam(val userId: UUID)

data class CreateUserParam(val email: String, val username: String? = null)

data class AuthenticateUserParam(val email: String)

data class UpdateUserParam(val userId: UUID, val username: String, val requestingUserId: UUID)
