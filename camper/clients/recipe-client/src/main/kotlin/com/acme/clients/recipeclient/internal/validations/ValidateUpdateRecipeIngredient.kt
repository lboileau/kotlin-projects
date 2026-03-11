package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.UpdateRecipeIngredientParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateRecipeIngredient {
    private val logger = LoggerFactory.getLogger(ValidateUpdateRecipeIngredient::class.java)

    fun execute(param: UpdateRecipeIngredientParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateRecipeIngredientParam): Result<Unit, AppError> {
        if (param.quantity != null && param.quantity <= java.math.BigDecimal.ZERO) return failure(ValidationError("quantity", "must be greater than 0"))
        if (param.unit != null && param.unit !in ValidateAddRecipeIngredient.VALID_UNITS) return failure(ValidationError("unit", "must be one of: ${ValidateAddRecipeIngredient.VALID_UNITS.joinToString()}"))
        if (param.status != null && param.status !in ValidateAddRecipeIngredient.VALID_STATUSES) return failure(ValidationError("status", "must be one of: ${ValidateAddRecipeIngredient.VALID_STATUSES.joinToString()}"))
        return success(Unit)
    }
}
