package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipePhotoByIdParam
import com.acme.clients.recipeclient.internal.adapters.RecipePhotoRowAdapter
import com.acme.clients.recipeclient.model.RecipePhoto
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipePhotoById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipePhotoById::class.java)

    fun execute(param: GetRecipePhotoByIdParam): Result<RecipePhoto, AppError> {
        logger.debug("Fetching recipe photo id={}", param.id)
        val photo = jdbi.withHandle<RecipePhoto?, Exception> { handle ->
            handle.createQuery("SELECT ${RecipePhotoRowAdapter.COLUMNS} FROM recipe_photos WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> RecipePhotoRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return photo?.let { success(it) } ?: failure(NotFoundError("RecipePhoto", param.id.toString()))
    }
}
