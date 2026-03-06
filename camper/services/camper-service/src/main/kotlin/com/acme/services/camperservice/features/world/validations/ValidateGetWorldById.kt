package com.acme.services.camperservice.features.world.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.world.error.WorldError
import com.acme.services.camperservice.features.world.params.GetWorldByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetWorldById {
    private val logger = LoggerFactory.getLogger(ValidateGetWorldById::class.java)

    fun execute(param: GetWorldByIdParam): Result<Unit, WorldError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetWorldByIdParam): Result<Unit, WorldError> {
        return success(Unit)
    }
}
