package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.CreateItemParam
import org.slf4j.LoggerFactory

internal class ValidateCreateItem {
    private val logger = LoggerFactory.getLogger(ValidateCreateItem::class.java)

    fun execute(param: CreateItemParam): Result<Unit, ItemError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateItemParam): Result<Unit, ItemError> {
        if (param.name.isBlank()) return failure(ItemError.Invalid("name", "must not be blank"))
        if (param.quantity <= 0) return failure(ItemError.Invalid("quantity", "must be greater than 0"))
        if (param.ownerType != "plan" && param.ownerType != "user") return failure(ItemError.Invalid("ownerType", "must be 'plan' or 'user'"))
        return success(Unit)
    }
}
