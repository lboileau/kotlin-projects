package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.UpdateRecipeParam
import org.slf4j.LoggerFactory

internal class ValidateUpdateRecipe {
    private val logger = LoggerFactory.getLogger(ValidateUpdateRecipe::class.java)

    fun execute(param: UpdateRecipeParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: UpdateRecipeParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
