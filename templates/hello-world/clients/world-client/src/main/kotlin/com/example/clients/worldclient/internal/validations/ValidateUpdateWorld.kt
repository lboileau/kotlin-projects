package com.example.clients.worldclient.internal.validations

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.common.error.ValidationError
import com.example.clients.common.failure
import com.example.clients.common.success
import com.example.clients.worldclient.api.UpdateWorldParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateWorld {
    private val logger = LoggerFactory.getLogger(ValidateUpdateWorld::class.java)

    fun execute(param: UpdateWorldParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateWorldParam): Result<Unit, AppError> {
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.greeting != null && param.greeting.isBlank()) return failure(ValidationError("greeting", "must not be blank"))
        return success(Unit)
    }
}
