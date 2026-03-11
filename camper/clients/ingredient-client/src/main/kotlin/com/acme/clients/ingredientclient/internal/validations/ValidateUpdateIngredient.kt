package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.UpdateIngredientParam
import com.acme.clients.ingredientclient.internal.validations.ValidateCreateIngredient.Companion.VALID_CATEGORIES
import com.acme.clients.ingredientclient.internal.validations.ValidateCreateIngredient.Companion.VALID_UNITS
import org.slf4j.LoggerFactory

internal class ValidateUpdateIngredient {
    private val logger = LoggerFactory.getLogger(ValidateUpdateIngredient::class.java)

    fun execute(param: UpdateIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateIngredientParam): Result<Unit, AppError> {
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.category != null && param.category !in VALID_CATEGORIES) return failure(ValidationError("category", "must be one of: ${VALID_CATEGORIES.joinToString(", ")}"))
        if (param.defaultUnit != null && param.defaultUnit !in VALID_UNITS) return failure(ValidationError("defaultUnit", "must be one of: ${VALID_UNITS.joinToString(", ")}"))
        return success(Unit)
    }
}
