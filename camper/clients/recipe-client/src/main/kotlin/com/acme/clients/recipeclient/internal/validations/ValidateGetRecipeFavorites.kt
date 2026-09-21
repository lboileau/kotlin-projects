package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeFavoritesParam
import org.slf4j.LoggerFactory

/** Default validator — a recipe id is the only input and nothing about it is checkable here. */
internal class ValidateGetRecipeFavorites {
    private val logger = LoggerFactory.getLogger(ValidateGetRecipeFavorites::class.java)

    fun execute(param: GetRecipeFavoritesParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: GetRecipeFavoritesParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
