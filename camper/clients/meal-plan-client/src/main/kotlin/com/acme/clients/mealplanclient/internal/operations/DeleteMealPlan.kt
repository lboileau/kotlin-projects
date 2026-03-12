package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.DeleteMealPlanParam
import com.acme.clients.mealplanclient.internal.validations.ValidateDeleteMealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteMealPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteMealPlan::class.java)
    private val validate = ValidateDeleteMealPlan()

    fun execute(param: DeleteMealPlanParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting meal plan id={}", param.id)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM meal_plans WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (deleted > 0) success(Unit) else failure(NotFoundError("MealPlan", param.id.toString()))
    }
}
