package com.acme.clients.userclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.userclient.api.UpdateUserParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateUser {
    private val logger = LoggerFactory.getLogger(ValidateUpdateUser::class.java)

    fun execute(param: UpdateUserParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateUserParam): Result<Unit, AppError> {
        if (param.username.isBlank()) return failure(ValidationError("username", "must not be blank"))
        return success(Unit)
    }
}
