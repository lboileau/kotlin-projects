package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.FindSimilarParam
import org.slf4j.LoggerFactory

internal class ValidateFindSimilarRecipes {
    private val logger = LoggerFactory.getLogger(ValidateFindSimilarRecipes::class.java)

    fun execute(param: FindSimilarParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: FindSimilarParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
