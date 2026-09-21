package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.GetByCreatedByParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.ListMealPlansByCreatorParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateListMealPlansByCreator

internal class ListMealPlansByCreatorAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateListMealPlansByCreator()

    fun execute(param: ListMealPlansByCreatorParam): Result<List<MealPlanResponse>, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        if (param.createdBy != param.userId) {
            return Result.Failure(MealPlanError.CreatedByFilterForbidden(param.userId))
        }

        return when (val result = mealPlanClient.getByCreatedBy(GetByCreatedByParam(param.createdBy))) {
            is Result.Success -> Result.Success(result.value.map { MealPlanMapper.toMealPlanResponse(it, param.userId) })
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("mealPlans", result.error.message))
        }
    }
}
