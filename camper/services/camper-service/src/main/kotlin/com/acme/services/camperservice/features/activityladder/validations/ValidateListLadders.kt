package com.acme.services.camperservice.features.activityladder.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import org.slf4j.LoggerFactory

internal class ValidateListLadders {
    private val logger = LoggerFactory.getLogger(ValidateListLadders::class.java)

    fun execute(param: ListLaddersParam): Result<Unit, LadderError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: ListLaddersParam): Result<Unit, LadderError> {
        return success(Unit)
    }
}
