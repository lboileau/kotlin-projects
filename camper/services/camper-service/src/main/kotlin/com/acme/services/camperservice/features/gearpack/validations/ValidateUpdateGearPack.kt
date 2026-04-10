package com.acme.services.camperservice.features.gearpack.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateGearPack {
    private val logger = LoggerFactory.getLogger(ValidateUpdateGearPack::class.java)

    fun execute(param: UpdateGearPackParam): Result<Unit, GearPackError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateGearPackParam): Result<Unit, GearPackError> {
        if (param.name != null) {
            if (param.name.isBlank()) {
                return failure(GearPackError.Invalid("name", "must not be blank"))
            }
            if (param.name.length > 100) {
                return failure(GearPackError.Invalid("name", "must be 100 characters or fewer"))
            }
        }
        if (param.description != null && param.description.length > 500) {
            return failure(GearPackError.Invalid("description", "must be 500 characters or fewer"))
        }
        return success(Unit)
    }
}
