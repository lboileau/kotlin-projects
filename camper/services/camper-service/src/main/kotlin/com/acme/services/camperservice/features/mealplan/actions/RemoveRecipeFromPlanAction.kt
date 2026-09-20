package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveRecipeFromPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveRecipeFromPlan
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.mealplanclient.api.RemoveRecipeFromPlanParam as ClientRemoveRecipeFromPlanParam

/**
 * Plan-level "remove recipe" for flat plans: removes every occurrence of the recipe from
 * the plan (all days, all meal types) in one call. Idempotent — 0 rows removed is still success.
 */
internal class RemoveRecipeFromPlanAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveRecipeFromPlan()

    /** Returns the number of rows removed (0 if the recipe wasn't in the plan). */
    fun execute(param: RemoveRecipeFromPlanParam): Result<Int, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> {}
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        return when (val result = mealPlanClient.removeRecipeFromPlan(
            ClientRemoveRecipeFromPlanParam(mealPlanId = param.mealPlanId, recipeId = param.recipeId)
        )) {
            is Result.Success -> Result.Success(result.value)
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
        }
    }
}
