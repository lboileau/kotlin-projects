package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.GetItemsByOwnerParam
import org.slf4j.LoggerFactory

internal class ValidateGetItemsByOwner {
    private val logger = LoggerFactory.getLogger(ValidateGetItemsByOwner::class.java)

    fun execute(param: GetItemsByOwnerParam): Result<Unit, ItemError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetItemsByOwnerParam): Result<Unit, ItemError> {
        if (param.ownerType != "plan" && param.ownerType != "user") return failure(ItemError.Invalid("ownerType", "must be 'plan' or 'user'"))
        return success(Unit)
    }
}
