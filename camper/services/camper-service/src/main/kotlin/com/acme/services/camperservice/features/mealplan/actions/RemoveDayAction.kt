package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveDayParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveDay
import com.acme.clients.mealplanclient.api.RemoveDayParam as ClientRemoveDayParam

internal class RemoveDayAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveDay()

    fun execute(param: RemoveDayParam): Result<Unit, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.removeDay(ClientRemoveDayParam(param.dayId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.DayNotFound(param.dayId))
                else -> Result.Failure(MealPlanError.Invalid("day", result.error.message))
            }
        }
    }
}
