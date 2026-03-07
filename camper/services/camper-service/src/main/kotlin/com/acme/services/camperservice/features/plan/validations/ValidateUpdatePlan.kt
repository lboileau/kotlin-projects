package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.UpdatePlanParam
import org.slf4j.LoggerFactory

internal class ValidateUpdatePlan {
    private val logger = LoggerFactory.getLogger(ValidateUpdatePlan::class.java)

    fun execute(param: UpdatePlanParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdatePlanParam): Result<Unit, PlanError> {
        if (param.name.isBlank()) return failure(PlanError.Invalid("name", "must not be blank"))
        return success(Unit)
    }
}
