package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import org.slf4j.LoggerFactory

/**
 * Default validator — an empty `recipeIds` is explicitly legal (the operation
 * short-circuits to an empty list rather than emitting an illegal `IN ()`), so
 * there is nothing to reject here.
 */
internal class ValidateGetRecipeFavoriteSummaries {
    private val logger = LoggerFactory.getLogger(ValidateGetRecipeFavoriteSummaries::class.java)

    fun execute(param: GetRecipeFavoriteSummariesParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetRecipeFavoriteSummariesParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
