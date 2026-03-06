package com.example.services.hello.features.world.validations

import com.example.clients.common.Result
import com.example.clients.common.failure
import com.example.clients.common.success
import com.example.services.hello.features.world.error.WorldError
import com.example.services.hello.features.world.params.CreateWorldParam
import org.slf4j.LoggerFactory

internal class ValidateCreateWorld {
    private val logger = LoggerFactory.getLogger(ValidateCreateWorld::class.java)

    fun execute(param: CreateWorldParam): Result<Unit, WorldError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateWorldParam): Result<Unit, WorldError> {
        if (param.name.isBlank()) return failure(WorldError.Invalid("name", "must not be blank"))
        if (param.greeting.isBlank()) return failure(WorldError.Invalid("greeting", "must not be blank"))
        return success(Unit)
    }
}
