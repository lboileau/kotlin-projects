package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import org.slf4j.LoggerFactory

internal class ValidateCastVote {
    private val logger = LoggerFactory.getLogger(ValidateCastVote::class.java)

    fun execute(param: CastVoteParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CastVoteParam): Result<Unit, LadderError> {
        // State-based checks (participant eligibility, match validity, duplicate vote) are
        // performed in the action body where state is available.
        return success(Unit)
    }
}
