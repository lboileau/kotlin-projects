package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.GetTemplatesParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetTemplates

internal class GetTemplatesAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateGetTemplates()

    fun execute(param: GetTemplatesParam): Result<List<MealPlanResponse>, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.getTemplates()) {
            is Result.Success -> Result.Success(result.value.map { MealPlanMapper.toMealPlanResponse(it) })
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("templates", result.error.message))
        }
    }
}
