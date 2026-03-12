package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRecipeRowAdapter
import com.acme.clients.mealplanclient.internal.validations.ValidateGetRecipesByDayId
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipesByDayId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipesByDayId::class.java)
    private val validate = ValidateGetRecipesByDayId()

    fun execute(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching recipes for day dayId={}", param.mealPlanDayId)
        val entities = jdbi.withHandle<List<MealPlanRecipe>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at
                FROM meal_plan_recipes WHERE meal_plan_day_id = :mealPlanDayId ORDER BY meal_type, created_at
                """.trimIndent()
            )
                .bind("mealPlanDayId", param.mealPlanDayId)
                .map { rs, _ -> MealPlanRecipeRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
