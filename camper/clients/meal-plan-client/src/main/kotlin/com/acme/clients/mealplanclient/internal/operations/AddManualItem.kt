package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddManualItemParam
import com.acme.clients.mealplanclient.internal.adapters.ShoppingListManualItemRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateAddManualItem
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddManualItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddManualItem::class.java)
    private val validate = ValidateAddManualItem()

    fun execute(param: AddManualItemParam): Result<ShoppingListManualItem, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding manual item mealPlanId={} ingredientId={} description={}", param.mealPlanId, param.ingredientId, param.description)
        return try {
            val entity = jdbi.withHandle<ShoppingListManualItem, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createQuery(
                    """
                    INSERT INTO shopping_list_manual_items (id, meal_plan_id, ingredient_id, description, quantity, unit, quantity_purchased, created_at, updated_at)
                    VALUES (:id, :mealPlanId, :ingredientId, :description, :quantity, :unit, 0, :createdAt, :updatedAt)
                    RETURNING id, meal_plan_id, ingredient_id, description, quantity, unit, quantity_purchased, created_at, updated_at
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("mealPlanId", param.mealPlanId)
                    .bind("ingredientId", param.ingredientId)
                    .bind("description", param.description)
                    .bind("quantity", param.quantity)
                    .bind("unit", param.unit)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .map { rs, _ -> ShoppingListManualItemRowAdapter.fromResultSet(rs) }
                    .one()
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_manual_items_meal_plan_ingredient_unit") == true) {
                failure(ConflictError("ShoppingListManualItem", "ingredient ${param.ingredientId} with unit ${param.unit} already exists for this meal plan"))
            } else {
                throw e
            }
        }
    }
}
