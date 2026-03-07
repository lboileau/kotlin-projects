package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.CreateUserParam
import org.slf4j.LoggerFactory

internal class ValidateCreateUser {
    private val logger = LoggerFactory.getLogger(ValidateCreateUser::class.java)

    fun execute(param: CreateUserParam): Result<Unit, UserError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateUserParam): Result<Unit, UserError> {
        if (param.email.isBlank()) return failure(UserError.Invalid("email", "must not be blank"))
        return success(Unit)
    }
}
