package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.CreatePlanParam
import org.slf4j.LoggerFactory

internal class ValidateCreatePlan {
    private val logger = LoggerFactory.getLogger(ValidateCreatePlan::class.java)

    fun execute(param: CreatePlanParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreatePlanParam): Result<Unit, PlanError> {
        if (param.name.isBlank()) return failure(PlanError.Invalid("name", "must not be blank"))
        return success(Unit)
    }
}
