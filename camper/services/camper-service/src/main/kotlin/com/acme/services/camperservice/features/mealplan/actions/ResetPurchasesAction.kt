package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.ResetPurchasesParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateResetPurchases
import com.acme.clients.mealplanclient.api.DeletePurchasesParam as ClientDeletePurchasesParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

internal class ResetPurchasesAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateResetPurchases()

    fun execute(param: ResetPurchasesParam): Result<Unit, MealPlanError> {
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

        return when (val result = mealPlanClient.deletePurchases(ClientDeletePurchasesParam(param.mealPlanId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("purchases", result.error.message))
        }
    }
}
