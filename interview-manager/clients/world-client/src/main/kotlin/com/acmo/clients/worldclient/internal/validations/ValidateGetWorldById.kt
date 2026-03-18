package com.acmo.clients.worldclient.internal.validations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.GetByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetWorldById {
    private val logger = LoggerFactory.getLogger(ValidateGetWorldById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
