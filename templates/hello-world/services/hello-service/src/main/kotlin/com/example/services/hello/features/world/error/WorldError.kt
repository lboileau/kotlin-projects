package com.example.services.hello.features.world.error

import com.example.clients.common.error.AppError
import com.example.clients.common.error.ConflictError
import com.example.clients.common.error.NotFoundError
import com.example.clients.common.error.ValidationError

sealed class WorldError(override val message: String) : AppError {
    data class NotFound(val entityId: String) : WorldError("World not found: $entityId")
    data class AlreadyExists(val name: String) : WorldError("World already exists: $name")
    data class Invalid(val field: String, val reason: String) : WorldError("Invalid world $field: $reason")

    companion object {
        fun fromClientError(error: AppError): WorldError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> AlreadyExists(error.detail)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
