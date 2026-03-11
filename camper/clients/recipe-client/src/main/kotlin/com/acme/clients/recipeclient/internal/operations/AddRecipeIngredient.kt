package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import com.acme.clients.recipeclient.internal.adapters.RecipeIngredientRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredient
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddRecipeIngredient(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipeIngredient::class.java)
    private val validate = ValidateAddRecipeIngredient()
    private val mapper = jacksonObjectMapper()

    fun execute(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding ingredient to recipe id={}", param.recipeId)
        val entity = jdbi.withHandle<RecipeIngredient, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            val flagsJson = mapper.writeValueAsString(param.reviewFlags)
            handle.createUpdate(
                """
                INSERT INTO recipe_ingredients
                    (id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at)
                VALUES
                    (:id, :recipeId, :ingredientId, :originalText, :quantity, :unit, :status, :matchedIngredientId, :suggestedIngredientName, :reviewFlags::jsonb, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("recipeId", param.recipeId)
                .bind("ingredientId", param.ingredientId)
                .bind("originalText", param.originalText)
                .bind("quantity", param.quantity)
                .bind("unit", param.unit)
                .bind("status", param.status)
                .bind("matchedIngredientId", param.matchedIngredientId)
                .bind("suggestedIngredientName", param.suggestedIngredientName)
                .bind("reviewFlags", flagsJson)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            handle.createQuery(
                "SELECT id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at FROM recipe_ingredients WHERE id = :id"
            )
                .bind("id", id)
                .map { rs, _ -> RecipeIngredientRowAdapter.fromResultSet(rs) }
                .one()
        }
        return success(entity)
    }
}
