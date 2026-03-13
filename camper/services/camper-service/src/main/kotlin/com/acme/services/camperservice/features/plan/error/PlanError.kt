package com.acme.services.camperservice.features.plan.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError

sealed class PlanError(override val message: String) : AppError {
    data class NotFound(val planId: String) : PlanError("Plan not found: $planId")
    data class NotOwner(val planId: String, val userId: String) : PlanError("User $userId is not the owner of plan $planId")
    data class AlreadyMember(val planId: String, val email: String) : PlanError("User $email is already a member of plan $planId")
    data class NotMember(val planId: String, val userId: String) : PlanError("User $userId is not a member of plan $planId")
    data class Invalid(val field: String, val reason: String) : PlanError("Invalid plan $field: $reason")
    data class CannotChangeOwnerRole(val planId: String) : PlanError("Cannot change the owner's role for plan $planId")

    companion object {
        fun fromClientError(error: AppError): PlanError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> AlreadyMember(error.entity, error.detail)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
