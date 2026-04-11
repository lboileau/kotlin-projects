package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import org.slf4j.LoggerFactory

internal class ValidateSetLadderWinner {
    private val logger = LoggerFactory.getLogger(ValidateSetLadderWinner::class.java)

    fun execute(param: SetLadderWinnerParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: SetLadderWinnerParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
