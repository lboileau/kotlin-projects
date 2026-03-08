package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.item.params.DeleteItemParam
import org.slf4j.LoggerFactory

internal class ValidateDeleteItem {
    private val logger = LoggerFactory.getLogger(ValidateDeleteItem::class.java)

    fun execute(param: DeleteItemParam): Result<Unit, ItemError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: DeleteItemParam): Result<Unit, ItemError> {
        TODO()
    }
}
