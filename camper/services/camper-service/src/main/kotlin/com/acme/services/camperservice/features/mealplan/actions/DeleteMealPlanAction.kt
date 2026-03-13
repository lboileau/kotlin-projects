package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.DeleteMealPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateDeleteMealPlan
import com.acme.clients.mealplanclient.api.DeleteMealPlanParam as ClientDeleteMealPlanParam

internal class DeleteMealPlanAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateDeleteMealPlan()

    fun execute(param: DeleteMealPlanParam): Result<Unit, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.delete(ClientDeleteMealPlanParam(param.mealPlanId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }
    }
}
