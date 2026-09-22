package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam
import org.slf4j.LoggerFactory

internal class ValidateReplaceRecipeSteps {
    private val logger = LoggerFactory.getLogger(ValidateReplaceRecipeSteps::class.java)

    fun execute(param: ReplaceRecipeStepsParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: ReplaceRecipeStepsParam): Result<Unit, AppError> {
        param.texts.forEachIndexed { i, text ->
            if (text.isBlank()) return failure(ValidationError("texts[$i]", "must not be blank"))
        }
        return success(Unit)
    }
}
