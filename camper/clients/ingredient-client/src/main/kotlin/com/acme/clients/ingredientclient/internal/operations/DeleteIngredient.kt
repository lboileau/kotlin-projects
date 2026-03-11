package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.DeleteIngredientParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteIngredient(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteIngredient::class.java)

    fun execute(param: DeleteIngredientParam): Result<Unit, AppError> {
        logger.debug("Deleting ingredient id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM ingredients WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("Ingredient", param.id.toString()))
    }
}
