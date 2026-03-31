package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddManualItemParam
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class ValidateAddManualItem {
    private val logger = LoggerFactory.getLogger(ValidateAddManualItem::class.java)

    fun execute(param: AddManualItemParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddManualItemParam): Result<Unit, AppError> {
        if (param.ingredientId == null && param.description == null) {
            return failure(ValidationError("ingredientId/description", "either ingredientId or description must be provided"))
        }
        if (param.ingredientId != null && param.description != null) {
            return failure(ValidationError("ingredientId/description", "only one of ingredientId or description may be provided"))
        }
        if (param.quantity <= BigDecimal.ZERO) {
            return failure(ValidationError("quantity", "must be greater than 0"))
        }
        if (param.ingredientId != null) {
            if (param.unit == null) return failure(ValidationError("unit", "must be provided for ingredient-based items"))
            if (param.unit !in VALID_UNITS) return failure(ValidationError("unit", "must be one of: ${VALID_UNITS.joinToString(", ")}"))
        }
        if (param.description != null) {
            if (param.description.isBlank()) return failure(ValidationError("description", "must not be blank"))
            if (param.description.length > 500) return failure(ValidationError("description", "must not exceed 500 characters"))
        }
        return success(Unit)
    }

    companion object {
        val VALID_UNITS = setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb", "pieces", "whole", "bunch", "can", "clove", "pinch", "slice", "sprig")
    }
}
