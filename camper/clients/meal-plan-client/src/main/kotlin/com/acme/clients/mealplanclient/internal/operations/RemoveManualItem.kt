package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.RemoveManualItemParam
import com.acme.clients.mealplanclient.internal.validations.ValidateRemoveManualItem
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Removes a manual item, scoped to [RemoveManualItemParam.mealPlanId] — an item id that exists
 * but belongs to a different meal plan is treated the same as a nonexistent one (`NotFoundError`).
 */
internal class RemoveManualItem(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveManualItem::class.java)
    private val validate = ValidateRemoveManualItem()

    fun execute(param: RemoveManualItemParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing manual item id={} mealPlanId={}", param.id, param.mealPlanId)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM shopping_list_manual_items WHERE id = :id AND meal_plan_id = :mealPlanId")
                .bind("id", param.id)
                .bind("mealPlanId", param.mealPlanId)
                .execute()
        }
        return if (deleted > 0) success(Unit) else failure(NotFoundError("ShoppingListManualItem", param.id.toString()))
    }
}
