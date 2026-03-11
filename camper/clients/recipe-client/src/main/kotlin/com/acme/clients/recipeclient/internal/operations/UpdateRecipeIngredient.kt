package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.UpdateRecipeIngredientParam
import com.acme.clients.recipeclient.internal.adapters.RecipeIngredientRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipeIngredient
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateRecipeIngredient(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateRecipeIngredient::class.java)
    private val validate = ValidateUpdateRecipeIngredient()
    private val mapper = jacksonObjectMapper()

    fun execute(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating recipe ingredient id={}", param.id)
        val entity = jdbi.withHandle<RecipeIngredient?, Exception> { handle ->
            val now = Instant.now()
            val sets = mutableListOf<String>()
            if (param.ingredientId != null) sets.add("ingredient_id = :ingredientId")
            if (param.quantity != null) sets.add("quantity = :quantity")
            if (param.unit != null) sets.add("unit = :unit")
            if (param.status != null) sets.add("status = :status")
            when {
                param.clearMatchedIngredient -> sets.add("matched_ingredient_id = NULL")
                param.matchedIngredientId != null -> sets.add("matched_ingredient_id = :matchedIngredientId")
            }
            if (param.suggestedIngredientName != null) sets.add("suggested_ingredient_name = :suggestedIngredientName")
            if (param.reviewFlags != null) sets.add("review_flags = :reviewFlags::jsonb")
            sets.add("updated_at = :updatedAt")

            val updated = handle.createUpdate("UPDATE recipe_ingredients SET ${sets.joinToString(", ")} WHERE id = :id")
                .bind("id", param.id)
                .also { q -> if (param.ingredientId != null) q.bind("ingredientId", param.ingredientId) }
                .also { q -> if (param.quantity != null) q.bind("quantity", param.quantity) }
                .also { q -> if (param.unit != null) q.bind("unit", param.unit) }
                .also { q -> if (param.status != null) q.bind("status", param.status) }
                .also { q -> if (param.matchedIngredientId != null && !param.clearMatchedIngredient) q.bind("matchedIngredientId", param.matchedIngredientId) }
                .also { q -> if (param.suggestedIngredientName != null) q.bind("suggestedIngredientName", param.suggestedIngredientName) }
                .also { q -> if (param.reviewFlags != null) q.bind("reviewFlags", mapper.writeValueAsString(param.reviewFlags)) }
                .bind("updatedAt", now)
                .execute()

            if (updated == 0) return@withHandle null

            handle.createQuery(
                "SELECT id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at FROM recipe_ingredients WHERE id = :id"
            )
                .bind("id", param.id)
                .map { rs, _ -> RecipeIngredientRowAdapter.fromResultSet(rs) }
                .one()
        }
        return if (entity != null) success(entity) else failure(NotFoundError("RecipeIngredient", param.id.toString()))
    }
}
