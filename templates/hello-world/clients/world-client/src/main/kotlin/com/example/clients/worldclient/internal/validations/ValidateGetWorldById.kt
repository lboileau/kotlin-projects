package com.example.clients.worldclient.internal.validations

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.common.success
import com.example.clients.worldclient.api.GetByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetWorldById {
    private val logger = LoggerFactory.getLogger(ValidateGetWorldById::class.java)

    fun execute(param: GetByIdParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
