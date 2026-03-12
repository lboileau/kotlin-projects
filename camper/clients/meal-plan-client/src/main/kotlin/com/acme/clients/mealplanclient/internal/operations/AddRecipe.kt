package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddRecipeParam
import com.acme.clients.mealplanclient.internal.validations.ValidateAddRecipe
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddRecipe(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipe::class.java)
    private val validate = ValidateAddRecipe()

    fun execute(param: AddRecipeParam): Result<MealPlanRecipe, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding recipe to day dayId={} mealType={} recipeId={}", param.mealPlanDayId, param.mealType, param.recipeId)
        val entity = jdbi.withHandle<MealPlanRecipe, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO meal_plan_recipes (id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at)
                VALUES (:id, :mealPlanDayId, :mealType, :recipeId, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("mealPlanDayId", param.mealPlanDayId)
                .bind("mealType", param.mealType)
                .bind("recipeId", param.recipeId)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            MealPlanRecipe(
                id = id,
                mealPlanDayId = param.mealPlanDayId,
                mealType = param.mealType,
                recipeId = param.recipeId,
                createdAt = now,
                updatedAt = now,
            )
        }
        return success(entity)
    }
}
