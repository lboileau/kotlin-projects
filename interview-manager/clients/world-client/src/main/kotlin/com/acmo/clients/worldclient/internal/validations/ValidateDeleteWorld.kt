package com.acmo.clients.worldclient.internal.validations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.DeleteWorldParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteWorld {
    private val logger = LoggerFactory.getLogger(ValidateDeleteWorld::class.java)

    fun execute(param: DeleteWorldParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: DeleteWorldParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
