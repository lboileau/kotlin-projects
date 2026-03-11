package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.RemoveRecipeIngredientParam
import com.acme.clients.recipeclient.internal.validations.ValidateRemoveRecipeIngredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveRecipeIngredient(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveRecipeIngredient::class.java)
    private val validate = ValidateRemoveRecipeIngredient()

    fun execute(param: RemoveRecipeIngredientParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing recipe ingredient id={}", param.id)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM recipe_ingredients WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (deleted > 0) success(Unit) else failure(NotFoundError("RecipeIngredient", param.id.toString()))
    }
}
