package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetPurchasesParam
import com.acme.clients.mealplanclient.internal.adapters.ShoppingListPurchaseRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetPurchases
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetPurchases(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetPurchases::class.java)
    private val validate = ValidateGetPurchases()

    fun execute(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching purchases for meal plan mealPlanId={}", param.mealPlanId)
        val entities = jdbi.withHandle<List<ShoppingListPurchase>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, meal_plan_id, ingredient_id, unit, quantity_purchased, created_at, updated_at
                FROM shopping_list_purchases WHERE meal_plan_id = :mealPlanId ORDER BY created_at
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .map { rs, _ -> ShoppingListPurchaseRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
