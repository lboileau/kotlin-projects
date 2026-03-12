package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetRecipesByMealPlanIdParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRecipeRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetRecipesByMealPlanId
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipesByMealPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipesByMealPlanId::class.java)
    private val validate = ValidateGetRecipesByMealPlanId()

    fun execute(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching all recipes for meal plan mealPlanId={}", param.mealPlanId)
        val entities = jdbi.withHandle<List<MealPlanRecipe>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT mpr.id, mpr.meal_plan_day_id, mpr.meal_type, mpr.recipe_id, mpr.created_at, mpr.updated_at
                FROM meal_plan_recipes mpr
                JOIN meal_plan_days mpd ON mpd.id = mpr.meal_plan_day_id
                WHERE mpd.meal_plan_id = :mealPlanId
                ORDER BY mpd.day_number, mpr.meal_type, mpr.created_at
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .map { rs, _ -> MealPlanRecipeRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
