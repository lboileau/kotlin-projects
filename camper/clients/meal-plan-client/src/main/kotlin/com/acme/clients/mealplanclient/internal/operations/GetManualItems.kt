package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetManualItemsParam
import com.acme.clients.mealplanclient.internal.adapters.ShoppingListManualItemRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetManualItems
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetManualItems(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetManualItems::class.java)
    private val validate = ValidateGetManualItems()

    fun execute(param: GetManualItemsParam): Result<List<ShoppingListManualItem>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching manual items for meal plan mealPlanId={}", param.mealPlanId)
        val entities = jdbi.withHandle<List<ShoppingListManualItem>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, meal_plan_id, ingredient_id, description, quantity, unit, quantity_purchased, created_at, updated_at
                FROM shopping_list_manual_items WHERE meal_plan_id = :mealPlanId ORDER BY created_at
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .map { rs, _ -> ShoppingListManualItemRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
