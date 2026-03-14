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

    companion object {
        val ALLOWED_EXPERIENCE_LEVELS = setOf("beginner", "intermediate", "advanced", "expert")
    }

    fun execute(param: UpdateUserParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateUserParam): Result<Unit, AppError> {
        if (param.username.isBlank()) return failure(ValidationError("username", "must not be blank"))
        if (param.experienceLevel != null && param.experienceLevel !in ALLOWED_EXPERIENCE_LEVELS) {
            return failure(ValidationError("experienceLevel", "must be one of: ${ALLOWED_EXPERIENCE_LEVELS.joinToString(", ")}"))
        }
        return success(Unit)
    }
}
