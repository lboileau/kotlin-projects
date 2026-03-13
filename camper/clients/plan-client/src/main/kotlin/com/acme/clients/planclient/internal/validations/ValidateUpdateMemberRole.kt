package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.UpdateMemberRoleParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateMemberRole {
    private val logger = LoggerFactory.getLogger(ValidateUpdateMemberRole::class.java)

    fun execute(param: UpdateMemberRoleParam): Result<Unit, AppError> {
        if (param.role !in setOf("member", "manager")) {
            logger.warn("Validation failed: role must be 'member' or 'manager'")
            return Result.Failure(ValidationError("role", "must be 'member' or 'manager'"))
        }
        return success(Unit)
    }
}
