package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import org.slf4j.LoggerFactory

internal class ValidateGetLadderById {
    private val logger = LoggerFactory.getLogger(ValidateGetLadderById::class.java)

    fun execute(param: GetLadderByIdParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetLadderByIdParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
