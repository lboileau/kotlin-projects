package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.GetItemParam
import org.slf4j.LoggerFactory

internal class ValidateGetItem {
    private val logger = LoggerFactory.getLogger(ValidateGetItem::class.java)

    fun execute(param: GetItemParam): Result<Unit, ItemError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetItemParam): Result<Unit, ItemError> {
        return success(Unit)
    }
}
