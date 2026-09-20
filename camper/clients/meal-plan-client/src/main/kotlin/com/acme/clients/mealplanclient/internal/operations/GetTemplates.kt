package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetTemplates(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetTemplates::class.java)

    fun execute(): Result<List<MealPlan>, AppError> {
        logger.debug("Fetching template meal plans")
        val entities = jdbi.withHandle<List<MealPlan>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at,
                    (SELECT COUNT(DISTINCT mpr.recipe_id) FROM meal_plan_recipes mpr JOIN meal_plan_days d ON d.id = mpr.meal_plan_day_id WHERE d.meal_plan_id = meal_plans.id) AS recipe_count
                FROM meal_plans WHERE is_template = true ORDER BY name
                """.trimIndent()
            )
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
