package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.FindRecipeIngredientsByIngredientIdParam
import com.acme.clients.recipeclient.internal.adapters.RecipeIngredientRowAdapter
import com.acme.clients.recipeclient.model.RecipeIngredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class FindRecipeIngredientsByIngredientId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(FindRecipeIngredientsByIngredientId::class.java)

    fun execute(param: FindRecipeIngredientsByIngredientIdParam): Result<List<RecipeIngredient>, AppError> {
        logger.debug("Finding recipe ingredients referencing ingredient id={}", param.ingredientId)
        val entities = jdbi.withHandle<List<RecipeIngredient>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, recipe_id, ingredient_id, original_text, quantity, unit, status,
                       matched_ingredient_id, suggested_ingredient_name, suggested_category, suggested_unit, review_flags, created_at, updated_at
                FROM recipe_ingredients
                WHERE ingredient_id = :ingredientId
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("ingredientId", param.ingredientId)
                .map { rs, _ -> RecipeIngredientRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
