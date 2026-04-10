package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import org.slf4j.LoggerFactory

internal class ValidateCountVotesForRound {
    private val logger = LoggerFactory.getLogger(ValidateCountVotesForRound::class.java)

    fun execute(param: CountVotesForRoundParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CountVotesForRoundParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
