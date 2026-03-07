package com.acme.services.camperservice.features.user.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.params.UpdateUserParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateUser {
    private val logger = LoggerFactory.getLogger(ValidateUpdateUser::class.java)

    fun execute(param: UpdateUserParam): Result<Unit, UserError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateUserParam): Result<Unit, UserError> {
        if (param.username.isBlank()) return failure(UserError.Invalid("username", "must not be blank"))
        return success(Unit)
    }
}
