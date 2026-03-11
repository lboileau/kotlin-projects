package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.internal.adapters.RecipeIngredientRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeIngredients
import com.acme.clients.recipeclient.model.RecipeIngredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipeIngredients(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipeIngredients::class.java)
    private val validate = ValidateGetRecipeIngredients()

    fun execute(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching ingredients for recipe id={}", param.recipeId)
        val entities = jdbi.withHandle<List<RecipeIngredient>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, recipe_id, ingredient_id, original_text, quantity, unit, status,
                       matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at
                FROM recipe_ingredients
                WHERE recipe_id = :recipeId
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> RecipeIngredientRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
