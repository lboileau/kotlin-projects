package com.acme.services.camperservice.features.gearpack.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import org.slf4j.LoggerFactory

internal class ValidateGetGearPack {
    private val logger = LoggerFactory.getLogger(ValidateGetGearPack::class.java)

    fun execute(param: GetGearPackParam): Result<Unit, GearPackError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetGearPackParam): Result<Unit, GearPackError> {
        return success(Unit)
    }
}
