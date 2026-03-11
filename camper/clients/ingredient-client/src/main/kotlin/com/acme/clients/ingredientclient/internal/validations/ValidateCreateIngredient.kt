package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.CreateIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateCreateIngredient {
    private val logger = LoggerFactory.getLogger(ValidateCreateIngredient::class.java)

    fun execute(param: CreateIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateIngredientParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.category !in VALID_CATEGORIES) return failure(ValidationError("category", "must be one of: ${VALID_CATEGORIES.joinToString(", ")}"))
        if (param.defaultUnit !in VALID_UNITS) return failure(ValidationError("defaultUnit", "must be one of: ${VALID_UNITS.joinToString(", ")}"))
        return success(Unit)
    }

    companion object {
        val VALID_CATEGORIES = setOf("produce", "dairy", "meat", "seafood", "pantry", "spice", "condiment", "frozen", "bakery", "other")
        val VALID_UNITS = setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb", "pieces", "whole", "bunch", "can", "clove", "pinch", "slice", "sprig")
    }
}
