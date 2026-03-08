package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.UpdateAssignmentParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateUpdateAssignment {
    private val logger = LoggerFactory.getLogger(ValidateUpdateAssignment::class.java)

    fun execute(param: UpdateAssignmentParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateAssignmentParam): Result<Unit, AppError> {
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.maxOccupancy != null && param.maxOccupancy <= 0) return failure(ValidationError("maxOccupancy", "must be greater than 0"))
        return success(Unit)
    }
}
