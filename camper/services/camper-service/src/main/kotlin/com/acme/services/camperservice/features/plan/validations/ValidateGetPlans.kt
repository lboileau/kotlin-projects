package com.acme.services.camperservice.features.plan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.GetPlansParam
import org.slf4j.LoggerFactory

internal class ValidateGetPlans {
    private val logger = LoggerFactory.getLogger(ValidateGetPlans::class.java)

    fun execute(param: GetPlansParam): Result<Unit, PlanError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetPlansParam): Result<Unit, PlanError> {
        return success(Unit)
    }
}
