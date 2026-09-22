package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipePhotoParam
import com.acme.clients.recipeclient.internal.adapters.RecipePhotoRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipePhoto
import com.acme.clients.recipeclient.model.RecipePhoto
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class AddRecipePhoto(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipePhoto::class.java)
    private val validate = ValidateAddRecipePhoto()

    fun execute(param: AddRecipePhotoParam): Result<RecipePhoto, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding photo id={} to recipe id={}", param.id, param.recipeId)
        val photo = jdbi.inTransaction<RecipePhoto, Exception> { handle ->
            // Next position, computed inside the transaction; the unique index catches a race.
            handle.createUpdate(
                """
                INSERT INTO recipe_photos
                    (id, recipe_id, position, storage_key, media_type, byte_size, width, height, source, role, created_by, created_at)
                VALUES
                    (:id, :recipeId,
                     (SELECT COALESCE(MAX(position), -1) + 1 FROM recipe_photos WHERE recipe_id = :recipeId),
                     :storageKey, :mediaType, :byteSize, :width, :height, :source, :role, :createdBy, :createdAt)
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("recipeId", param.recipeId)
                .bind("storageKey", param.storageKey)
                .bind("mediaType", param.mediaType)
                .bind("byteSize", param.byteSize)
                .bind("width", param.width)
                .bind("height", param.height)
                .bind("source", param.source)
                .bind("role", param.role)
                .bind("createdBy", param.createdBy)
                .bind("createdAt", Instant.now())
                .execute()

            handle.createQuery("SELECT ${RecipePhotoRowAdapter.COLUMNS} FROM recipe_photos WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> RecipePhotoRowAdapter.fromResultSet(rs) }
                .one()
        }
        return success(photo)
    }
}
