package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeFavoriteParam
import org.slf4j.LoggerFactory

/**
 * Default validator — nothing about favouriting is checkable from the param alone.
 * Whether the recipe is visible to the user is a service-layer concern, and whether
 * the row already exists is settled by the unique constraint.
 */
internal class ValidateAddRecipeFavorite {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipeFavorite::class.java)

    fun execute(param: AddRecipeFavoriteParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipeFavoriteParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
