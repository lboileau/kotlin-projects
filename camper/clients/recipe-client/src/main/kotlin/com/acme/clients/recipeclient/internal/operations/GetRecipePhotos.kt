package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipePhotosParam
import com.acme.clients.recipeclient.internal.adapters.RecipePhotoRowAdapter
import com.acme.clients.recipeclient.model.RecipePhoto
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipePhotos(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipePhotos::class.java)

    fun execute(param: GetRecipePhotosParam): Result<List<RecipePhoto>, AppError> {
        logger.debug("Fetching photos for recipe id={}", param.recipeId)
        val photos = jdbi.withHandle<List<RecipePhoto>, Exception> { handle ->
            handle.createQuery("SELECT ${RecipePhotoRowAdapter.COLUMNS} FROM recipe_photos WHERE recipe_id = :recipeId ORDER BY position")
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> RecipePhotoRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(photos)
    }
}
