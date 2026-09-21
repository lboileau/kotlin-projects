package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetByIdParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.internal.adapters.MealPlanSelect
import com.acme.clients.mealplanclient.internal.validations.ValidateGetMealPlanById
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetMealPlanById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetMealPlanById::class.java)
    private val validate = ValidateGetMealPlanById()

    fun execute(param: GetByIdParam): Result<MealPlan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding meal plan by id={}", param.id)
        val entity = jdbi.withHandle<MealPlan?, Exception> { handle ->
            handle.createQuery("SELECT ${MealPlanSelect.COLUMNS} FROM meal_plans WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("MealPlan", param.id.toString()))
    }
}
