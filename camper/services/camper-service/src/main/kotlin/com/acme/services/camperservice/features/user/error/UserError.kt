package com.acme.services.camperservice.features.user.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError

sealed class UserError(override val message: String) : AppError {
    data class NotFound(val email: String) : UserError("User not found: $email")
    data class Invalid(val field: String, val reason: String) : UserError("Invalid user $field: $reason")
    data class Forbidden(val userId: String) : UserError("Forbidden: user $userId cannot perform this action")
    data class RegistrationRequired(val email: String) : UserError("Please register with a trail name to continue")

    companion object {
        fun fromClientError(error: AppError): UserError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
