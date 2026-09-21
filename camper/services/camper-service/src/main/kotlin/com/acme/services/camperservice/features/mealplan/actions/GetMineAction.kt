package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.GetMineParam as ClientGetMineParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.GetMineParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetMine

/** Meal plans the caller owns plus meal plans shared with them, newest updated first (templates included). */
internal class GetMineAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateGetMine()

    fun execute(param: GetMineParam): Result<List<MealPlanResponse>, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.getMine(ClientGetMineParam(param.userId))) {
            is Result.Success -> Result.Success(result.value.map { MealPlanMapper.toMealPlanResponse(it, param.userId) })
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("mealPlans", result.error.message))
        }
    }
}
