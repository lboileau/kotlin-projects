package com.acme.clients.gearpackclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.UpdateGearPackItemParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateGearPackItem {
    private val logger = LoggerFactory.getLogger(ValidateUpdateGearPackItem::class.java)

    fun execute(param: UpdateGearPackItemParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateGearPackItemParam): Result<Unit, AppError> {
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.category != null && param.category.isBlank()) return failure(ValidationError("category", "must not be blank"))
        if (param.defaultQuantity != null && param.defaultQuantity < 1) return failure(ValidationError("defaultQuantity", "must be at least 1"))
        return success(Unit)
    }
}
