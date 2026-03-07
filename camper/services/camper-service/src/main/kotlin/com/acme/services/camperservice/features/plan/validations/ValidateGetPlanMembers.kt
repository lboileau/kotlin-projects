package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.GetPlanMembersParam
import org.slf4j.LoggerFactory

internal class ValidateGetPlanMembers {
    private val logger = LoggerFactory.getLogger(ValidateGetPlanMembers::class.java)

    fun execute(param: GetPlanMembersParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetPlanMembersParam): Result<Unit, PlanError> {
        return success(Unit)
    }
}
