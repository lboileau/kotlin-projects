package com.acme.services.camperservice.features.item.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import java.util.UUID

sealed class ItemError(override val message: String) : AppError {
    data class NotFound(val itemId: UUID) : ItemError("Item not found: $itemId")
    data class Invalid(val field: String, val reason: String) : ItemError("Invalid item $field: $reason")

    companion object {
        fun fromClientError(error: AppError): ItemError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
