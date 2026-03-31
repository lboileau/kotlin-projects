package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveManualItemParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveManualItem
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.mealplanclient.api.RemoveManualItemParam as ClientRemoveManualItemParam

internal class RemoveManualItemAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveManualItem()

    fun execute(param: RemoveManualItemParam): Result<Unit, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Verify meal plan exists
        when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> {}
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        return when (val result = mealPlanClient.removeManualItem(ClientRemoveManualItemParam(param.itemId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.ManualItemNotFound(param.itemId))
                else -> Result.Failure(MealPlanError.Invalid("manualItem", result.error.message))
            }
        }
    }
}
