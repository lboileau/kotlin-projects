package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.internal.adapters.RecipeRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeById
import com.acme.clients.recipeclient.model.Recipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipeById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipeById::class.java)
    private val validate = ValidateGetRecipeById()

    fun execute(param: GetByIdParam): Result<Recipe, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding recipe by id={}", param.id)
        val entity = jdbi.withHandle<Recipe?, Exception> { handle ->
            handle.createQuery(
                "SELECT id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, meal, theme, created_at, updated_at FROM recipes WHERE id = :id"
            )
                .bind("id", param.id)
                .map { rs, _ -> RecipeRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Recipe", param.id.toString()))
    }
}
