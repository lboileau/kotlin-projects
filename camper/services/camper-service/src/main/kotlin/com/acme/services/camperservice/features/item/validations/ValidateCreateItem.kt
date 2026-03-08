package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
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
        TODO()
    }
}
