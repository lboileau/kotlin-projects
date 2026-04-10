package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.CreateLadderParam
import org.slf4j.LoggerFactory

internal class ValidateCreateLadder {
    private val logger = LoggerFactory.getLogger(ValidateCreateLadder::class.java)

    fun execute(param: CreateLadderParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateLadderParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
