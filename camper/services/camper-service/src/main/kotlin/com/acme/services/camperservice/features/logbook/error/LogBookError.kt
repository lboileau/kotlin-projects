package com.acme.services.camperservice.features.logbook.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import java.util.UUID

sealed class LogBookError(override val message: String) : AppError {
    data class NotFound(val entityId: UUID) : LogBookError("Log book entry not found: $entityId")
    data class Invalid(val field: String, val reason: String) : LogBookError("Invalid log book $field: $reason")
    data class Forbidden(val planId: UUID, val userId: UUID) : LogBookError("User $userId is not authorized for this action in plan $planId")

    companion object {
        fun fromClientError(error: AppError): LogBookError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
