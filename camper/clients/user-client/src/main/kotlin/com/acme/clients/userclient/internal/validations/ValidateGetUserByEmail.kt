package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.GetByEmailParam
import org.slf4j.LoggerFactory

internal class ValidateGetUserByEmail {
    private val logger = LoggerFactory.getLogger(ValidateGetUserByEmail::class.java)

    fun execute(param: GetByEmailParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetByEmailParam): Result<Unit, AppError> {
        if (param.email.isBlank()) return failure(ValidationError("email", "must not be blank"))
        return success(Unit)
    }
}
