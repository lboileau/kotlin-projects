package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.UpdateItemParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateItem {
    private val logger = LoggerFactory.getLogger(ValidateUpdateItem::class.java)

    fun execute(param: UpdateItemParam): Result<Unit, ItemError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateItemParam): Result<Unit, ItemError> {
        if (param.name.isBlank()) return failure(ItemError.Invalid("name", "must not be blank"))
        if (param.quantity <= 0) return failure(ItemError.Invalid("quantity", "must be greater than 0"))
        return success(Unit)
    }
}
