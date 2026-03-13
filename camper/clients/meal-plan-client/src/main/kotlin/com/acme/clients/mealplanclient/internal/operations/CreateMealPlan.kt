package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.CreateMealPlanParam
import com.acme.clients.mealplanclient.internal.validations.ValidateCreateMealPlan
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateMealPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateMealPlan::class.java)
    private val validate = ValidateCreateMealPlan()

    fun execute(param: CreateMealPlanParam): Result<MealPlan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating meal plan name={} isTemplate={}", param.name, param.isTemplate)
        return try {
            val entity = jdbi.withHandle<MealPlan, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO meal_plans (id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at)
                    VALUES (:id, :planId, :name, :servings, :scalingMode, :isTemplate, :sourceTemplateId, :createdBy, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("planId", param.planId)
                    .bind("name", param.name)
                    .bind("servings", param.servings)
                    .bind("scalingMode", param.scalingMode)
                    .bind("isTemplate", param.isTemplate)
                    .bind("sourceTemplateId", param.sourceTemplateId)
                    .bind("createdBy", param.createdBy)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                MealPlan(
                    id = id,
                    planId = param.planId,
                    name = param.name,
                    servings = param.servings,
                    scalingMode = param.scalingMode,
                    isTemplate = param.isTemplate,
                    sourceTemplateId = param.sourceTemplateId,
                    createdBy = param.createdBy,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_meal_plans_plan_id") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("MealPlan", "plan_id '${param.planId}' already has a meal plan"))
            } else {
                throw e
            }
        }
    }
}
