package com.acme.services.camperservice.features.assignment.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError

sealed class AssignmentError(override val message: String) : AppError {
    data class NotFound(val assignmentId: String) : AssignmentError("Assignment not found: $assignmentId")
    data class NotOwner(val assignmentId: String, val userId: String) : AssignmentError("User $userId is not the owner of assignment $assignmentId")
    data class Invalid(val field: String, val reason: String) : AssignmentError("Invalid $field: $reason")
    data class AtCapacity(val assignmentId: String) : AssignmentError("Assignment $assignmentId is at capacity")
    data class AlreadyAssigned(val userId: String, val type: String, val planId: String) : AssignmentError("User $userId is already assigned to a $type in plan $planId")
    data class AlreadyMember(val assignmentId: String, val userId: String) : AssignmentError("User $userId is already a member of assignment $assignmentId")
    data class CannotRemoveOwner(val assignmentId: String, val userId: String) : AssignmentError("Cannot remove owner $userId from assignment $assignmentId")
    data class PlanNotFound(val planId: String) : AssignmentError("Plan not found: $planId")

    companion object {
        fun fromClientError(error: AppError): AssignmentError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> when {
                error.detail.contains("already assigned") -> {
                    val parts = error.detail.split(" ")
                    AlreadyAssigned(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
                }
                error.detail.contains("already a member") -> AlreadyMember(error.entity, error.detail)
                else -> Invalid("unknown", error.message)
            }
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
