package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.FindSimilarParam
import com.acme.clients.recipeclient.internal.adapters.RecipeRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateFindSimilarRecipes
import com.acme.clients.recipeclient.model.Recipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class FindSimilarRecipes(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(FindSimilarRecipes::class.java)
    private val validate = ValidateFindSimilarRecipes()

    fun execute(param: FindSimilarParam): Result<List<Recipe>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding recipes similar to name={}", param.name)
        val entities = jdbi.withHandle<List<Recipe>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, meal, theme, created_at, updated_at
                FROM recipes
                WHERE LOWER(name) LIKE LOWER('%' || :name || '%')
                ORDER BY name
                """.trimIndent()
            )
                .bind("name", param.name)
                .map { rs, _ -> RecipeRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
