package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.DeleteMealPlanParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteMealPlan {
    private val logger = LoggerFactory.getLogger(ValidateDeleteMealPlan::class.java)

    fun execute(param: DeleteMealPlanParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: DeleteMealPlanParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
