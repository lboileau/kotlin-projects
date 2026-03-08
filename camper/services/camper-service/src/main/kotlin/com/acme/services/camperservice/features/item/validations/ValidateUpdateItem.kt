package com.acme.services.camperservice.features.item.validations

import com.acme.clients.common.Result
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
        TODO()
    }
}
