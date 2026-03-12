package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.UpdateMealPlanParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateMealPlan {
    private val logger = LoggerFactory.getLogger(ValidateUpdateMealPlan::class.java)

    fun execute(param: UpdateMealPlanParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateMealPlanParam): Result<Unit, AppError> {
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.servings != null && param.servings <= 0) return failure(ValidationError("servings", "must be greater than 0"))
        if (param.scalingMode != null && param.scalingMode !in VALID_SCALING_MODES) return failure(ValidationError("scalingMode", "must be one of: ${VALID_SCALING_MODES.joinToString()}"))
        return success(Unit)
    }

    companion object {
        val VALID_SCALING_MODES = setOf("fractional", "round_up")
    }
}
