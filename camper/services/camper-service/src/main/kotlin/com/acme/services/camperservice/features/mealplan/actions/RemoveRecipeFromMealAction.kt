package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveRecipeFromMealParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveRecipeFromMeal
import com.acme.clients.mealplanclient.api.RemoveRecipeParam as ClientRemoveRecipeParam

internal class RemoveRecipeFromMealAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveRecipeFromMeal()

    fun execute(param: RemoveRecipeFromMealParam): Result<Unit, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.removeRecipe(ClientRemoveRecipeParam(param.mealPlanRecipeId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.RecipeNotFound(param.mealPlanRecipeId))
                else -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
            }
        }
    }
}
