package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDayResponse
import com.acme.services.camperservice.features.mealplan.dto.MealsByTypeResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.AddDayParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateAddDay
import com.acme.clients.mealplanclient.api.AddDayParam as ClientAddDayParam

internal class AddDayAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateAddDay()

    fun execute(param: AddDayParam): Result<MealPlanDayResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val day = when (val result = mealPlanClient.addDay(
            ClientAddDayParam(
                mealPlanId = param.mealPlanId,
                dayNumber = param.dayNumber,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is ConflictError -> Result.Failure(MealPlanError.DuplicateDayNumber(param.dayNumber))
                else -> Result.Failure(MealPlanError.Invalid("day", result.error.message))
            }
        }

        return Result.Success(
            MealPlanDayResponse(
                id = day.id,
                dayNumber = day.dayNumber,
                meals = MealsByTypeResponse(
                    breakfast = emptyList(),
                    lunch = emptyList(),
                    dinner = emptyList(),
                    snack = emptyList(),
                ),
            )
        )
    }
}
