package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import org.slf4j.LoggerFactory

internal class ValidateGetLadderDetail {
    private val logger = LoggerFactory.getLogger(ValidateGetLadderDetail::class.java)

    fun execute(param: GetLadderDetailParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetLadderDetailParam): Result<Unit, LadderError> {
        return success(Unit)
    }
}
