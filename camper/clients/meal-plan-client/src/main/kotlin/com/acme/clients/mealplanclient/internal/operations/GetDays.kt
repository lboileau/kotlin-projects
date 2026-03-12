package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanDayRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetDays
import com.acme.clients.mealplanclient.model.MealPlanDay
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetDays(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetDays::class.java)
    private val validate = ValidateGetDays()

    fun execute(param: GetDaysParam): Result<List<MealPlanDay>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching days for meal plan mealPlanId={}", param.mealPlanId)
        val entities = jdbi.withHandle<List<MealPlanDay>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, meal_plan_id, day_number, created_at, updated_at
                FROM meal_plan_days WHERE meal_plan_id = :mealPlanId ORDER BY day_number
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .map { rs, _ -> MealPlanDayRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
