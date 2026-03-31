package com.acme.clients.mealplanclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.UpdateManualItemPurchaseParam
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class ValidateUpdateManualItemPurchase {
    private val logger = LoggerFactory.getLogger(ValidateUpdateManualItemPurchase::class.java)

    fun execute(param: UpdateManualItemPurchaseParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateManualItemPurchaseParam): Result<Unit, AppError> {
        if (param.quantityPurchased < BigDecimal.ZERO) return failure(ValidationError("quantityPurchased", "must be greater than or equal to 0"))
        return success(Unit)
    }
}
