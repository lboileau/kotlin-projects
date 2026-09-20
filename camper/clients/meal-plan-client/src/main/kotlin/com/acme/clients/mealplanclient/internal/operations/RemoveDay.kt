package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.RemoveDayParam
import com.acme.clients.mealplanclient.internal.validations.ValidateRemoveDay
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Removes a day, scoped to [RemoveDayParam.mealPlanId] — a day id that exists but belongs to a
 * different meal plan is treated the same as a nonexistent one (`NotFoundError`), so a caller who
 * only had `mealPlanId` authorized can't reach a day in a plan they don't have access to.
 */
internal class RemoveDay(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveDay::class.java)
    private val validate = ValidateRemoveDay()

    fun execute(param: RemoveDayParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing day id={} mealPlanId={}", param.id, param.mealPlanId)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM meal_plan_days WHERE id = :id AND meal_plan_id = :mealPlanId")
                .bind("id", param.id)
                .bind("mealPlanId", param.mealPlanId)
                .execute()
        }
        return if (deleted > 0) success(Unit) else failure(NotFoundError("MealPlanDay", param.id.toString()))
    }
}
