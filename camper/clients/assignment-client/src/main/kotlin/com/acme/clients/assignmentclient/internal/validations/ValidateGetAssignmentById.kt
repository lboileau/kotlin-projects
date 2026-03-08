package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateGetAssignmentById {
    private val logger = LoggerFactory.getLogger(ValidateGetAssignmentById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
