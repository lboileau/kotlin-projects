package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateAddRecipeIngredient {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipeIngredient::class.java)

    fun execute(param: AddRecipeIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipeIngredientParam): Result<Unit, AppError> {
        if (param.quantity <= java.math.BigDecimal.ZERO) return failure(ValidationError("quantity", "must be greater than 0"))
        if (param.unit !in VALID_UNITS) return failure(ValidationError("unit", "must be one of: ${VALID_UNITS.joinToString()}"))
        if (param.status !in VALID_STATUSES) return failure(ValidationError("status", "must be one of: ${VALID_STATUSES.joinToString()}"))
        return success(Unit)
    }

    companion object {
        val VALID_UNITS = setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb", "pieces", "whole", "bunch", "can", "clove", "pinch", "slice", "sprig")
        val VALID_STATUSES = setOf("pending_review", "approved")
    }
}
