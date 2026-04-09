package com.acme.services.camperservice.features.gearpack.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackItemParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateGearPackItem {
    private val logger = LoggerFactory.getLogger(ValidateUpdateGearPackItem::class.java)

    fun execute(param: UpdateGearPackItemParam): Result<Unit, GearPackError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateGearPackItemParam): Result<Unit, GearPackError> {
        if (param.name != null && param.name.isBlank()) {
            return failure(GearPackError.Invalid("name", "must not be blank"))
        }
        if (param.category != null && param.category.isBlank()) {
            return failure(GearPackError.Invalid("category", "must not be blank"))
        }
        if (param.defaultQuantity != null && param.defaultQuantity < 1) {
            return failure(GearPackError.Invalid("defaultQuantity", "must be at least 1"))
        }
        return success(Unit)
    }
}
