package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import org.slf4j.LoggerFactory

internal class ValidateStartLadder {
    private val logger = LoggerFactory.getLogger(ValidateStartLadder::class.java)

    fun execute(param: StartLadderParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: StartLadderParam): Result<Unit, LadderError> {
        // Activity count ≥ 2 check is done in the action after fetching activities
        // because it requires state from the DB, not from the param.
        return success(Unit)
    }
}
