package com.acmo.clients.worldclient.internal.validations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.GetListParam
import org.slf4j.LoggerFactory

internal class ValidateGetWorldList {
    private val logger = LoggerFactory.getLogger(ValidateGetWorldList::class.java)

    fun execute(param: GetListParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(@Suppress("UNUSED_PARAMETER") param: GetListParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
