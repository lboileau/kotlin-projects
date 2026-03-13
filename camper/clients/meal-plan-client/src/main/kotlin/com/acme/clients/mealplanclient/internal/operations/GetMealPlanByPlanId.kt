package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetByPlanIdParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetMealPlanByPlanId
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetMealPlanByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetMealPlanByPlanId::class.java)
    private val validate = ValidateGetMealPlanByPlanId()

    fun execute(param: GetByPlanIdParam): Result<MealPlan?, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding meal plan by planId={}", param.planId)
        val entity = jdbi.withHandle<MealPlan?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at
                FROM meal_plans WHERE plan_id = :planId
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return success(entity)
    }
}
