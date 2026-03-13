package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.UpdateMemberRoleParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateMemberRole {
    private val logger = LoggerFactory.getLogger(ValidateUpdateMemberRole::class.java)

    fun execute(param: UpdateMemberRoleParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateMemberRoleParam): Result<Unit, PlanError> {
        if (param.role !in setOf("member", "manager")) {
            return Result.Failure(PlanError.Invalid("role", "must be 'member' or 'manager'"))
        }
        return success(Unit)
    }
}
