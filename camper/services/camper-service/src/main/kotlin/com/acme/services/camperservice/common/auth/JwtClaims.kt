package com.acme.services.camperservice.common.auth

import com.acme.clients.common.error.AppError
import java.util.UUID

data class JwtClaims(
    val externalId: UUID,
    val email: String
)

sealed class JwtError(override val message: String) : AppError {
    data object Expired : JwtError("Token expired")
    data object Invalid : JwtError("Invalid token")
}
