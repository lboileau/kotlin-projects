package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import com.acme.clients.recipeclient.internal.adapters.RecipeIngredientRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredients
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddRecipeIngredients(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipeIngredients::class.java)
    private val validate = ValidateAddRecipeIngredients()
    private val mapper = jacksonObjectMapper()

    fun execute(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.ingredients.isEmpty()) return success(emptyList())

        logger.debug("Adding {} ingredients to recipe", param.ingredients.size)
        val entities = jdbi.inTransaction<List<RecipeIngredient>, Exception> { handle ->
            param.ingredients.map { ing ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                val flagsJson = mapper.writeValueAsString(ing.reviewFlags)
                handle.createUpdate(
                    """
                    INSERT INTO recipe_ingredients
                        (id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, suggested_category, suggested_unit, review_flags, created_at, updated_at)
                    VALUES
                        (:id, :recipeId, CAST(:ingredientId AS uuid), :originalText, :quantity, :unit, :status, CAST(:matchedIngredientId AS uuid), :suggestedIngredientName, :suggestedCategory, :suggestedUnit, :reviewFlags::jsonb, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("recipeId", ing.recipeId)
                    .bind("ingredientId", ing.ingredientId?.toString())
                    .bind("originalText", ing.originalText)
                    .bind("quantity", ing.quantity)
                    .bind("unit", ing.unit)
                    .bind("status", ing.status)
                    .bind("matchedIngredientId", ing.matchedIngredientId?.toString())
                    .bind("suggestedIngredientName", ing.suggestedIngredientName)
                    .bind("suggestedCategory", ing.suggestedCategory)
                    .bind("suggestedUnit", ing.suggestedUnit)
                    .bind("reviewFlags", flagsJson)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()

                handle.createQuery(
                    "SELECT id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, suggested_category, suggested_unit, review_flags, created_at, updated_at FROM recipe_ingredients WHERE id = :id"
                )
                    .bind("id", id)
                    .map { rs, _ -> RecipeIngredientRowAdapter.fromResultSet(rs) }
                    .one()
            }
        }
        return success(entities)
    }
}
