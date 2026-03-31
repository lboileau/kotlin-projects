package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.UpdateManualItemPurchaseParam
import com.acme.clients.mealplanclient.internal.adapters.ShoppingListManualItemRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateUpdateManualItemPurchase
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class UpdateManualItemPurchase(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateManualItemPurchase::class.java)
    private val validate = ValidateUpdateManualItemPurchase()

    fun execute(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating manual item purchase id={} quantityPurchased={}", param.id, param.quantityPurchased)
        val entity = jdbi.withHandle<ShoppingListManualItem?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE shopping_list_manual_items
                SET quantity_purchased = :quantityPurchased, updated_at = now()
                WHERE id = :id
                RETURNING id, meal_plan_id, ingredient_id, description, quantity, unit, quantity_purchased, created_at, updated_at
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("quantityPurchased", param.quantityPurchased)
                .map { rs, _ -> ShoppingListManualItemRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("ShoppingListManualItem", param.id.toString()))
    }
}
