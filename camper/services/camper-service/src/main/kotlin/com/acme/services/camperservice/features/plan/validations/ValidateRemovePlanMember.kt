package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.RemovePlanMemberParam
import org.slf4j.LoggerFactory

internal class ValidateRemovePlanMember {
    private val logger = LoggerFactory.getLogger(ValidateRemovePlanMember::class.java)

    fun execute(param: RemovePlanMemberParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: RemovePlanMemberParam): Result<Unit, PlanError> {
        return success(Unit)
    }
}
