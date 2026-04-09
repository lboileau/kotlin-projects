package com.acme.clients.gearpackclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.SearchGearPackItemsParam
import org.slf4j.LoggerFactory

internal class ValidateSearchGearPackItems {
    private val logger = LoggerFactory.getLogger(ValidateSearchGearPackItems::class.java)

    fun execute(param: SearchGearPackItemsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: SearchGearPackItemsParam): Result<Unit, AppError> {
        if (param.query.isBlank()) return failure(ValidationError("query", "must not be blank"))
        return success(Unit)
    }
}
