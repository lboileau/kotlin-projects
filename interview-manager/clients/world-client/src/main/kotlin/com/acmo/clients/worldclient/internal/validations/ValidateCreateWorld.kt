package com.acmo.clients.worldclient.internal.validations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.ValidationError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.CreateWorldParam
import org.slf4j.LoggerFactory

internal class ValidateCreateWorld {
    private val logger = LoggerFactory.getLogger(ValidateCreateWorld::class.java)

    fun execute(param: CreateWorldParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateWorldParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.greeting.isBlank()) return failure(ValidationError("greeting", "must not be blank"))
        return success(Unit)
    }
}
