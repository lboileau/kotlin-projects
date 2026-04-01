package com.acme.services.camperservice.features.gearpack.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import java.util.UUID

sealed class GearPackError(override val message: String) : AppError {
    data class NotFound(val packId: UUID) : GearPackError("Gear pack not found: $packId")
    data class Invalid(val field: String, val reason: String) : GearPackError("Invalid $field: $reason")
    data class Forbidden(val planId: String, val userId: String) : GearPackError("User $userId is not authorized to apply gear packs to plan $planId")
    data class ApplyFailed(val packName: String, val reason: String) : GearPackError("Failed to apply gear pack '$packName': $reason")

    companion object {
        fun fromClientError(error: AppError): GearPackError = when (error) {
            is NotFoundError -> NotFound(UUID.fromString(error.id))
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
