package com.acme.clients.worldclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.worldclient.api.GetListParam
import org.slf4j.LoggerFactory

internal class ValidateGetWorldList {
    private val logger = LoggerFactory.getLogger(ValidateGetWorldList::class.java)

    fun execute(param: GetListParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetListParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
