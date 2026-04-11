package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import org.slf4j.LoggerFactory

internal class ValidateRemoveActivity {
    private val logger = LoggerFactory.getLogger(ValidateRemoveActivity::class.java)

    fun execute(param: RemoveActivityParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: RemoveActivityParam): Result<Unit, LadderError> {
        return success(Unit)
    }
}
