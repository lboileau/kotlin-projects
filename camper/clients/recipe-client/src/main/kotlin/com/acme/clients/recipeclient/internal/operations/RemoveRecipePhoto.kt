package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.RemoveRecipePhotoParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveRecipePhoto(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveRecipePhoto::class.java)

    fun execute(param: RemoveRecipePhotoParam): Result<Unit, AppError> {
        logger.debug("Removing recipe photo id={}", param.id)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM recipe_photos WHERE id = :id").bind("id", param.id).execute()
        }
        return if (deleted > 0) success(Unit) else failure(NotFoundError("RecipePhoto", param.id.toString()))
    }
}
