package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.DeletePurchasesParam
import com.acme.clients.mealplanclient.internal.validations.ValidateDeletePurchases
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeletePurchases(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeletePurchases::class.java)
    private val validate = ValidateDeletePurchases()

    fun execute(param: DeletePurchasesParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting all purchases for meal plan mealPlanId={}", param.mealPlanId)
        jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM shopping_list_purchases WHERE meal_plan_id = :mealPlanId")
                .bind("mealPlanId", param.mealPlanId)
                .execute()
        }
        return success(Unit)
    }
}
