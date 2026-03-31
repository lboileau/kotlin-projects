package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.ResetManualItemPurchasesParam
import com.acme.clients.mealplanclient.internal.validations.ValidateResetManualItemPurchases
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class ResetManualItemPurchases(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(ResetManualItemPurchases::class.java)
    private val validate = ValidateResetManualItemPurchases()

    fun execute(param: ResetManualItemPurchasesParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Resetting manual item purchases for meal plan mealPlanId={}", param.mealPlanId)
        jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                UPDATE shopping_list_manual_items
                SET quantity_purchased = 0, updated_at = now()
                WHERE meal_plan_id = :mealPlanId
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .execute()
        }
        return success(Unit)
    }
}
