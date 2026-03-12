package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.mealplanclient.api.GetByIdParam
import com.acme.clients.mealplanclient.api.UpdateMealPlanParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateUpdateMealPlan
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateMealPlan(
    private val jdbi: Jdbi,
    private val getMealPlanById: GetMealPlanById,
) {
    private val logger = LoggerFactory.getLogger(UpdateMealPlan::class.java)
    private val validate = ValidateUpdateMealPlan()

    fun execute(param: UpdateMealPlanParam): Result<MealPlan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = getMealPlanById.execute(GetByIdParam(param.id))
        if (existing is Result.Failure) return existing

        logger.debug("Updating meal plan id={}", param.id)
        val sets = mutableListOf<String>()
        if (param.name != null) sets.add("name = :name")
        if (param.servings != null) sets.add("servings = :servings")
        if (param.scalingMode != null) sets.add("scaling_mode = :scalingMode")
        sets.add("updated_at = :updatedAt")

        val entity = jdbi.withHandle<MealPlan, Exception> { handle ->
            val now = Instant.now()
            handle.createUpdate("UPDATE meal_plans SET ${sets.joinToString(", ")} WHERE id = :id")
                .bind("id", param.id)
                .also { q -> if (param.name != null) q.bind("name", param.name) }
                .also { q -> if (param.servings != null) q.bind("servings", param.servings) }
                .also { q -> if (param.scalingMode != null) q.bind("scalingMode", param.scalingMode) }
                .bind("updatedAt", now)
                .execute()

            handle.createQuery(
                """
                SELECT id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at
                FROM meal_plans WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .one()
        }
        return Result.Success(entity)
    }
}
