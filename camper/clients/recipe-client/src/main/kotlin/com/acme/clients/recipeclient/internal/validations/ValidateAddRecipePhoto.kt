package com.acme.clients.recipeclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipePhotoParam
import com.acme.clients.recipeclient.model.RecipePhoto
import org.slf4j.LoggerFactory

internal class ValidateAddRecipePhoto {
    private val logger = LoggerFactory.getLogger(ValidateAddRecipePhoto::class.java)

    fun execute(param: AddRecipePhotoParam): Result<Unit, AppError> {
        return validate(param).also { result ->
            result.errorOrNull()?.let { logger.warn("Validation failed: {}", it.message) }
        }
    }

    private fun validate(param: AddRecipePhotoParam): Result<Unit, AppError> {
        if (param.storageKey.isBlank()) return failure(ValidationError("storageKey", "must not be blank"))
        if (param.mediaType.isBlank()) return failure(ValidationError("mediaType", "must not be blank"))
        if (param.byteSize <= 0) return failure(ValidationError("byteSize", "must be positive"))
        if (param.source !in RecipePhoto.SOURCES) return failure(ValidationError("source", "must be one of ${RecipePhoto.SOURCES}"))
        if (param.role != null && param.role !in RecipePhoto.ROLES) return failure(ValidationError("role", "must be one of ${RecipePhoto.ROLES}"))
        return success(Unit)
    }
}
