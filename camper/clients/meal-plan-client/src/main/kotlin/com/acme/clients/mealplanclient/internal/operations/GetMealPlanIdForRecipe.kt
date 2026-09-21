package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetMealPlanIdForRecipeParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.UUID

internal class GetMealPlanIdForRecipe(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetMealPlanIdForRecipe::class.java)

    fun execute(param: GetMealPlanIdForRecipeParam): Result<UUID, AppError> {
        logger.debug("Resolving meal plan id for mealPlanRecipeId={}", param.mealPlanRecipeId)
        val mealPlanId = jdbi.withHandle<UUID?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT mpd.meal_plan_id
                FROM meal_plan_recipes mpr
                JOIN meal_plan_days mpd ON mpr.meal_plan_day_id = mpd.id
                WHERE mpr.id = :id
                """.trimIndent()
            )
                .bind("id", param.mealPlanRecipeId)
                .mapTo(UUID::class.java)
                .findOne()
                .orElse(null)
        }
        return if (mealPlanId != null) success(mealPlanId) else failure(NotFoundError("MealPlanRecipe", param.mealPlanRecipeId.toString()))
    }
}
