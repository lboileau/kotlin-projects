package com.acme.clients.gearpackclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.CreateGearPackParam
import org.slf4j.LoggerFactory

internal class ValidateCreateGearPack {
    private val logger = LoggerFactory.getLogger(ValidateCreateGearPack::class.java)

    fun execute(param: CreateGearPackParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateGearPackParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.name.length > 100) return failure(ValidationError("name", "must not exceed 100 characters"))
        if (param.description.length > 500) return failure(ValidationError("description", "must not exceed 500 characters"))
        return success(Unit)
    }
}
