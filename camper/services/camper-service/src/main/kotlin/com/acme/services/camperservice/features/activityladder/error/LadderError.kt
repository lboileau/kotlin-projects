package com.acme.services.camperservice.features.activityladder.error

import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import java.util.UUID

sealed class LadderError(override val message: String) : AppError {

    data class NotFound(val ladderId: String) : LadderError("Ladder not found: $ladderId")
    data class NotCreator(val ladderId: UUID, val userId: UUID) :
        LadderError("User $userId is not creator of ladder $ladderId")
    data class Invalid(val field: String, val reason: String) : LadderError("Invalid $field: $reason")
    data class IllegalState(val expected: String, val actual: String) :
        LadderError("Illegal state: expected $expected, was $actual")
    data class NotEligibleVoter(val userId: UUID) : LadderError("User $userId is not an eligible voter")
    data class AlreadyVoted(val userId: UUID, val roundNumber: Int) :
        LadderError("User $userId already voted in round $roundNumber")
    data class InvalidVoteTarget(val activityId: UUID) :
        LadderError("Activity $activityId is not in the current match")
    data class NotEnoughActivities(val count: Int) :
        LadderError("Cannot start ladder: need 2+ activities, have $count")
    data class NoPresentUsers(val ladderId: UUID) :
        LadderError("Cannot start ladder: no users currently present")

    companion object {
        fun fromClientError(error: AppError): LadderError = when (error) {
            is NotFoundError -> NotFound(error.id)
            is ConflictError -> Invalid(error.entity, error.detail)
            is ValidationError -> Invalid(error.field, error.reason)
            else -> Invalid("unknown", error.message)
        }
    }
}
