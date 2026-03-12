package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.UpsertPurchaseParam
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class ValidateUpsertPurchase {
    private val logger = LoggerFactory.getLogger(ValidateUpsertPurchase::class.java)

    fun execute(param: UpsertPurchaseParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpsertPurchaseParam): Result<Unit, AppError> {
        if (param.unit.isBlank()) return failure(ValidationError("unit", "must not be blank"))
        if (param.quantityPurchased < BigDecimal.ZERO) return failure(ValidationError("quantityPurchased", "must be greater than or equal to 0"))
        return success(Unit)
    }
}
