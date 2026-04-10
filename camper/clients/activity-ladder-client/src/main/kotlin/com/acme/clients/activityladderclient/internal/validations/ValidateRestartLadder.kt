package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.RestartLadderParam
import org.slf4j.LoggerFactory

internal class ValidateRestartLadder {
    private val logger = LoggerFactory.getLogger(ValidateRestartLadder::class.java)

    fun execute(param: RestartLadderParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: RestartLadderParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
