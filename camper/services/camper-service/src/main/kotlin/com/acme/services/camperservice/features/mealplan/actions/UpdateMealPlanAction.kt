package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.UpdateMealPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateUpdateMealPlan
import com.acme.clients.mealplanclient.api.UpdateMealPlanParam as ClientUpdateMealPlanParam

internal class UpdateMealPlanAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateUpdateMealPlan()

    fun execute(param: UpdateMealPlanParam): Result<MealPlanResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val updated = when (val result = mealPlanClient.update(
            ClientUpdateMealPlanParam(
                id = param.mealPlanId,
                name = param.name,
                servings = param.servings,
                scalingMode = param.scalingMode,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        return Result.Success(MealPlanMapper.toMealPlanResponse(updated))
    }
}
