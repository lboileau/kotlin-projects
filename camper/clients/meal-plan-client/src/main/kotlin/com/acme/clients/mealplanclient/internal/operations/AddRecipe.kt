package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddRecipeParam
import com.acme.clients.mealplanclient.internal.validations.ValidateAddRecipe
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Adds a recipe to a day, scoped to [AddRecipeParam.mealPlanId] — the insert only happens if
 * `mealPlanDayId` actually belongs to that meal plan (`WHERE EXISTS`), so a caller who only had
 * `mealPlanId` authorized can't reach a day in a plan they don't have access to. 0 rows inserted
 * (day belongs elsewhere, or doesn't exist) is reported as `NotFoundError` for the day.
 */
internal class AddRecipe(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipe::class.java)
    private val validate = ValidateAddRecipe()

    fun execute(param: AddRecipeParam): Result<MealPlanRecipe, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug(
            "Adding recipe to day dayId={} mealPlanId={} mealType={} recipeId={}",
            param.mealPlanDayId, param.mealPlanId, param.mealType, param.recipeId
        )
        val entity = jdbi.withHandle<MealPlanRecipe?, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            val inserted = handle.createUpdate(
                """
                INSERT INTO meal_plan_recipes (id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at)
                SELECT :id, :mealPlanDayId, :mealType, :recipeId, :createdAt, :updatedAt
                WHERE EXISTS (SELECT 1 FROM meal_plan_days WHERE id = :mealPlanDayId AND meal_plan_id = :mealPlanId)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("mealPlanDayId", param.mealPlanDayId)
                .bind("mealPlanId", param.mealPlanId)
                .bind("mealType", param.mealType)
                .bind("recipeId", param.recipeId)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            if (inserted == 0) {
                null
            } else {
                MealPlanRecipe(
                    id = id,
                    mealPlanDayId = param.mealPlanDayId,
                    mealType = param.mealType,
                    recipeId = param.recipeId,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
        return if (entity != null) success(entity) else failure(NotFoundError("MealPlanDay", param.mealPlanDayId.toString()))
    }
}
