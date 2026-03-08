package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.AddAssignmentMemberParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateAddAssignmentMember {
    private val logger = LoggerFactory.getLogger(ValidateAddAssignmentMember::class.java)

    fun execute(param: AddAssignmentMemberParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
