package com.acmo.services.interviewservice.features.world.validations

import com.acmo.clients.common.Result
import com.acmo.clients.common.success
import com.acmo.services.interviewservice.features.world.error.WorldError
import com.acmo.services.interviewservice.features.world.params.GetWorldByIdParam
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
