package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.AuthenticateUserParam
import org.slf4j.LoggerFactory

internal class ValidateAuthenticateUser {
    private val logger = LoggerFactory.getLogger(ValidateAuthenticateUser::class.java)

    fun execute(param: AuthenticateUserParam): Result<Unit, UserError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AuthenticateUserParam): Result<Unit, UserError> {
        if (param.email.isBlank()) return failure(UserError.Invalid("email", "must not be blank"))
        return success(Unit)
    }
}
