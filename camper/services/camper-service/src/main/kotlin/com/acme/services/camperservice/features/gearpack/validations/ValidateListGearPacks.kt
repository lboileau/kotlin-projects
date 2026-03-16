package com.acme.services.camperservice.features.gearpack.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import org.slf4j.LoggerFactory

internal class ValidateListGearPacks {
    private val logger = LoggerFactory.getLogger(ValidateListGearPacks::class.java)

    fun execute(param: ListGearPacksParam): Result<Unit, GearPackError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: ListGearPacksParam): Result<Unit, GearPackError> {
        return success(Unit)
    }
}
