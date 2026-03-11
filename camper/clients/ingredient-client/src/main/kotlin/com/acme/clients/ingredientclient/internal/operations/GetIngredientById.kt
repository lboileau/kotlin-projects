package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.GetByIdParam
import com.acme.clients.ingredientclient.internal.adapters.IngredientRowAdapter
import com.acme.clients.ingredientclient.internal.validations.ValidateGetIngredientById
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetIngredientById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetIngredientById::class.java)
    private val validate = ValidateGetIngredientById()

    fun execute(param: GetByIdParam): Result<Ingredient, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding ingredient by id={}", param.id)
        val entity = jdbi.withHandle<Ingredient?, Exception> { handle ->
            handle.createQuery(
                "SELECT id, name, category, default_unit, created_at, updated_at FROM ingredients WHERE id = :id"
            )
                .bind("id", param.id)
                .map { rs, _ -> IngredientRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Ingredient", param.id.toString()))
    }
}
