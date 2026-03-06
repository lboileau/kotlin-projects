package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.AddPlanMemberParam
import org.slf4j.LoggerFactory

internal class ValidateAddPlanMember {
    private val logger = LoggerFactory.getLogger(ValidateAddPlanMember::class.java)

    fun execute(param: AddPlanMemberParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddPlanMemberParam): Result<Unit, PlanError> {
        if (param.email.isBlank()) return failure(PlanError.Invalid("email", "must not be blank"))
        return success(Unit)
    }
}
