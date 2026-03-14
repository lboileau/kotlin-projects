package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetDietaryRestrictionsParam
import org.slf4j.LoggerFactory

internal class ValidateGetDietaryRestrictions {
    private val logger = LoggerFactory.getLogger(ValidateGetDietaryRestrictions::class.java)

    fun execute(param: GetDietaryRestrictionsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetDietaryRestrictionsParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
