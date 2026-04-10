package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import org.slf4j.LoggerFactory

internal class ValidateRestartLadder {
    private val logger = LoggerFactory.getLogger(ValidateRestartLadder::class.java)

    fun execute(param: RestartLadderParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: RestartLadderParam): Result<Unit, LadderError> {
        // Status check (must be ACTIVE or COMPLETED) is done in the action after fetching the ladder.
        return success(Unit)
    }
}
