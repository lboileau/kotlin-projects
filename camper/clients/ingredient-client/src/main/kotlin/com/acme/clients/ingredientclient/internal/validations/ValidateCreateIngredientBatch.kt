package com.acme.clients.ingredientclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.CreateBatchParam
import org.slf4j.LoggerFactory

internal class ValidateCreateIngredientBatch {
    private val logger = LoggerFactory.getLogger(ValidateCreateIngredientBatch::class.java)

    fun execute(param: CreateBatchParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateBatchParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
