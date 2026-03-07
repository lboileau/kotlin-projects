package com.acme.clients.planclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.CreatePlanParam
import org.slf4j.LoggerFactory

internal class ValidateCreatePlan {
    private val logger = LoggerFactory.getLogger(ValidateCreatePlan::class.java)

    fun execute(param: CreatePlanParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreatePlanParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.visibility !in listOf("public", "private")) return failure(ValidationError("visibility", "must be 'public' or 'private'"))
        return success(Unit)
    }
}
