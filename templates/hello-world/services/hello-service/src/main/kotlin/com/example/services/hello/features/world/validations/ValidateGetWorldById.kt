package com.example.services.hello.features.world.validations

import com.example.clients.common.Result
import com.example.clients.common.success
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.params.GetWorldByIdParam
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
