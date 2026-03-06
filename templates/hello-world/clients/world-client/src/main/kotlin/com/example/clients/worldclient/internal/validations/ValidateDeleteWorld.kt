package com.example.clients.worldclient.internal.validations

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.common.success
import com.example.clients.worldclient.api.DeleteWorldParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteWorld {
    private val logger = LoggerFactory.getLogger(ValidateDeleteWorld::class.java)

    fun execute(param: DeleteWorldParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: DeleteWorldParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
