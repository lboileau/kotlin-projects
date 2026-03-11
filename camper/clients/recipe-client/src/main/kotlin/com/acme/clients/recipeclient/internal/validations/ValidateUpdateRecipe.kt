package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
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
        if (param.name != null && param.name.isBlank()) return failure(ValidationError("name", "must not be blank"))
        if (param.baseServings != null && param.baseServings <= 0) return failure(ValidationError("baseServings", "must be greater than 0"))
        if (param.status != null && param.status !in ValidateCreateRecipe.VALID_STATUSES) return failure(ValidationError("status", "must be one of: ${ValidateCreateRecipe.VALID_STATUSES.joinToString()}"))
        return success(Unit)
    }
}
