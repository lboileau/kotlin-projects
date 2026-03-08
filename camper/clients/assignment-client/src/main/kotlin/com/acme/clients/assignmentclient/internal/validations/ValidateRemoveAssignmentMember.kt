package com.acme.clients.assignmentclient.internal.validations

import com.acme.clients.assignmentclient.api.RemoveAssignmentMemberParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.slf4j.LoggerFactory

internal class ValidateRemoveAssignmentMember {
    private val logger = LoggerFactory.getLogger(ValidateRemoveAssignmentMember::class.java)

    fun execute(param: RemoveAssignmentMemberParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
