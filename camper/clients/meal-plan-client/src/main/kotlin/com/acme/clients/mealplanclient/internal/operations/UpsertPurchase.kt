package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.UpsertPurchaseParam
import com.acme.clients.mealplanclient.internal.adapters.ShoppingListPurchaseRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateUpsertPurchase
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class UpsertPurchase(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpsertPurchase::class.java)
    private val validate = ValidateUpsertPurchase()

    fun execute(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Upserting purchase mealPlanId={} ingredientId={} unit={}", param.mealPlanId, param.ingredientId, param.unit)
        val entity = jdbi.withHandle<ShoppingListPurchase, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO shopping_list_purchases (id, meal_plan_id, ingredient_id, unit, quantity_purchased, created_at, updated_at)
                VALUES (:id, :mealPlanId, :ingredientId, :unit, :quantityPurchased, :createdAt, :updatedAt)
                ON CONFLICT (meal_plan_id, ingredient_id, unit)
                DO UPDATE SET quantity_purchased = EXCLUDED.quantity_purchased, updated_at = now()
                """.trimIndent()
            )
                .bind("id", id)
                .bind("mealPlanId", param.mealPlanId)
                .bind("ingredientId", param.ingredientId)
                .bind("unit", param.unit)
                .bind("quantityPurchased", param.quantityPurchased)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            handle.createQuery(
                """
                SELECT id, meal_plan_id, ingredient_id, unit, quantity_purchased, created_at, updated_at
                FROM shopping_list_purchases
                WHERE meal_plan_id = :mealPlanId AND ingredient_id = :ingredientId AND unit = :unit
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .bind("ingredientId", param.ingredientId)
                .bind("unit", param.unit)
                .map { rs, _ -> ShoppingListPurchaseRowAdapter.fromResultSet(rs) }
                .one()
        }
        return success(entity)
    }
}
