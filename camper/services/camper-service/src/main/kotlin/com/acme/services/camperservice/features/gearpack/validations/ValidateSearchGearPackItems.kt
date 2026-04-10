package com.acme.services.camperservice.features.gearpack.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.params.SearchGearPackItemsParam
import org.slf4j.LoggerFactory

internal class ValidateSearchGearPackItems {
    private val logger = LoggerFactory.getLogger(ValidateSearchGearPackItems::class.java)

    fun execute(param: SearchGearPackItemsParam): Result<Unit, GearPackError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: SearchGearPackItemsParam): Result<Unit, GearPackError> {
        if (param.query.isBlank()) {
            return failure(GearPackError.Invalid("q", "must not be blank"))
        }
        return success(Unit)
    }
}
