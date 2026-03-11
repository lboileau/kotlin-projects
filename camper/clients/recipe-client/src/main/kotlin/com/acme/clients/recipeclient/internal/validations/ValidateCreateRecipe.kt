package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.CreateRecipeParam
import org.slf4j.LoggerFactory

internal class ValidateCreateRecipe {
    private val logger = LoggerFactory.getLogger(ValidateCreateRecipe::class.java)

    fun execute(param: CreateRecipeParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: CreateRecipeParam): Result<Unit, AppError> {
        if (param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.baseServings <= 0) return failure(ValidationError("baseServings", "must be greater than 0"))
        if (param.status !in VALID_STATUSES) return failure(ValidationError("status", "must be one of: ${VALID_STATUSES.joinToString()}"))
        return success(Unit)
    }

    companion object {
        val VALID_STATUSES = setOf("draft", "published")
    }
}
