package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.CreateUserParam
import org.slf4j.LoggerFactory

internal class ValidateCreateUser {
    private val logger = LoggerFactory.getLogger(ValidateCreateUser::class.java)

    fun execute(param: CreateUserParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateUserParam): Result<Unit, AppError> {
        if (param.email.isBlank()) return failure(ValidationError("email", "must not be blank"))
        return success(Unit)
    }
}
