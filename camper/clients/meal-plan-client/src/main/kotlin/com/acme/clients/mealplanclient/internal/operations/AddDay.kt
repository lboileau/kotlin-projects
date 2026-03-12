package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddDayParam
import com.acme.clients.mealplanclient.internal.validations.ValidateAddDay
import com.acme.clients.mealplanclient.model.MealPlanDay
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddDay(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddDay::class.java)
    private val validate = ValidateAddDay()

    fun execute(param: AddDayParam): Result<MealPlanDay, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding day to meal plan mealPlanId={} dayNumber={}", param.mealPlanId, param.dayNumber)
        return try {
            val entity = jdbi.withHandle<MealPlanDay, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO meal_plan_days (id, meal_plan_id, day_number, created_at, updated_at)
                    VALUES (:id, :mealPlanId, :dayNumber, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("mealPlanId", param.mealPlanId)
                    .bind("dayNumber", param.dayNumber)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                MealPlanDay(
                    id = id,
                    mealPlanId = param.mealPlanId,
                    dayNumber = param.dayNumber,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_meal_plan_days_plan_day") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("MealPlanDay", "day_number ${param.dayNumber} already exists for this meal plan"))
            } else {
                throw e
            }
        }
    }
}
