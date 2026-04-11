package com.acme.clients.activityladderclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateLadderState {
    private val logger = LoggerFactory.getLogger(ValidateUpdateLadderState::class.java)

    fun execute(param: UpdateLadderStateParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateLadderStateParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
