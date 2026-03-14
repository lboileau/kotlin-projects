package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.SetDietaryRestrictionsParam
import org.slf4j.LoggerFactory

internal class ValidateSetDietaryRestrictions {
    private val logger = LoggerFactory.getLogger(ValidateSetDietaryRestrictions::class.java)

    fun execute(param: SetDietaryRestrictionsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: SetDietaryRestrictionsParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
