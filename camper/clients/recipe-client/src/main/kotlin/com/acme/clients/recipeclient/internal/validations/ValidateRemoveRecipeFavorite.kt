package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.RemoveRecipeFavoriteParam
import org.slf4j.LoggerFactory

/**
 * Default validator — removing a favourite is idempotent, so there is nothing to
 * reject from the param alone.
 */
internal class ValidateRemoveRecipeFavorite {
    private val logger = LoggerFactory.getLogger(ValidateRemoveRecipeFavorite::class.java)

    fun execute(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
