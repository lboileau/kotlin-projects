package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.RemoveRecipeFromPlanParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveRecipeFromPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveRecipeFromPlan::class.java)

    fun execute(param: RemoveRecipeFromPlanParam): Result<Int, AppError> {
        logger.debug("Removing recipe recipeId={} from every day of mealPlanId={}", param.recipeId, param.mealPlanId)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                DELETE FROM meal_plan_recipes
                WHERE recipe_id = :recipeId
                  AND meal_plan_day_id IN (SELECT id FROM meal_plan_days WHERE meal_plan_id = :mealPlanId)
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .bind("recipeId", param.recipeId)
                .execute()
        }
        return success(deleted)
    }
}
