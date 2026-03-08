package com.acme.clients.itemclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.CreateItemParam
import org.slf4j.LoggerFactory

internal class ValidateCreateItem {
    private val logger = LoggerFactory.getLogger(ValidateCreateItem::class.java)

    fun execute(param: CreateItemParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateItemParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.quantity <= 0) return failure(ValidationError("quantity", "must be greater than 0"))
        val hasPlanId = param.planId != null
        val hasUserId = param.userId != null
        if (hasPlanId == hasUserId) return failure(ValidationError("planId/userId", "exactly one of planId or userId must be provided"))
        return success(Unit)
    }
}
