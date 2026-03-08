package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.DeleteAssignmentParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateDeleteAssignment {
    private val logger = LoggerFactory.getLogger(ValidateDeleteAssignment::class.java)

    fun execute(param: DeleteAssignmentParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
