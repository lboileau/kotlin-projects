package com.acme.services.camperservice.features.world.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.world.error.WorldError
import com.acme.services.camperservice.features.world.params.GetAllWorldsParam
import org.slf4j.LoggerFactory

internal class ValidateGetAllWorlds {
    private val logger = LoggerFactory.getLogger(ValidateGetAllWorlds::class.java)

    fun execute(param: GetAllWorldsParam): Result<Unit, WorldError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetAllWorldsParam): Result<Unit, WorldError> {
        return success(Unit)
    }
}
