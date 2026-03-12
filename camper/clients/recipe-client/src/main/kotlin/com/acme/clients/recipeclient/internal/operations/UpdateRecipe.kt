package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.UpdateRecipeParam
import com.acme.clients.recipeclient.internal.adapters.RecipeRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipe
import com.acme.clients.recipeclient.model.Recipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateRecipe(
    private val jdbi: Jdbi,
    private val getRecipeById: GetRecipeById
) {
    private val logger = LoggerFactory.getLogger(UpdateRecipe::class.java)
    private val validate = ValidateUpdateRecipe()

    fun execute(param: UpdateRecipeParam): Result<Recipe, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = getRecipeById.execute(GetByIdParam(param.id))
        if (existing is Result.Failure) return existing

        logger.debug("Updating recipe id={}", param.id)
        return try {
            val sets = mutableListOf<String>()
            if (param.name != null) sets.add("name = :name")
            if (param.description != null) sets.add("description = :description")
            if (param.baseServings != null) sets.add("base_servings = :baseServings")
            if (param.status != null) sets.add("status = :status")
            when {
                param.clearDuplicateOf -> sets.add("duplicate_of_id = NULL")
                param.duplicateOfId != null -> sets.add("duplicate_of_id = :duplicateOfId")
            }
            if (param.meal != null) sets.add("meal = :meal")
            if (param.theme != null) sets.add("theme = :theme")
            sets.add("updated_at = :updatedAt")

            val entity = jdbi.withHandle<Recipe, Exception> { handle ->
                val now = Instant.now()
                handle.createUpdate("UPDATE recipes SET ${sets.joinToString(", ")} WHERE id = :id")
                    .bind("id", param.id)
                    .also { q -> if (param.name != null) q.bind("name", param.name) }
                    .also { q -> if (param.description != null) q.bind("description", param.description) }
                    .also { q -> if (param.baseServings != null) q.bind("baseServings", param.baseServings) }
                    .also { q -> if (param.status != null) q.bind("status", param.status) }
                    .also { q -> if (param.duplicateOfId != null && !param.clearDuplicateOf) q.bind("duplicateOfId", param.duplicateOfId) }
                    .also { q -> if (param.meal != null) q.bind("meal", param.meal) }
                    .also { q -> if (param.theme != null) q.bind("theme", param.theme) }
                    .bind("updatedAt", now)
                    .execute()

                handle.createQuery(
                    "SELECT id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, meal, theme, created_at, updated_at FROM recipes WHERE id = :id"
                )
                    .bind("id", param.id)
                    .map { rs, _ -> RecipeRowAdapter.fromResultSet(rs) }
                    .one()
            }
            Result.Success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Recipe", "update conflict"))
            } else {
                throw e
            }
        }
    }
}
