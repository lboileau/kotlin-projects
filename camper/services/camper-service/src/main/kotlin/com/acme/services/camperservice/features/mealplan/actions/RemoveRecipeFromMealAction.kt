package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.GetMealPlanIdForRecipeParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveRecipeFromMealParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveRecipeFromMeal
import java.util.UUID
import com.acme.clients.mealplanclient.api.RemoveRecipeParam as ClientRemoveRecipeParam

internal class RemoveRecipeFromMealAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveRecipeFromMeal()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    /** Removes the recipe and returns the owning meal plan's ID, so callers can publish a sync event. */
    fun execute(param: RemoveRecipeFromMealParam): Result<UUID, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val mealPlanId = when (val result = mealPlanClient.getMealPlanIdForRecipe(GetMealPlanIdForRecipeParam(param.mealPlanRecipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.RecipeNotFound(param.mealPlanRecipeId))
                else -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
            }
        }

        when (val access = authorizer.authorize(mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.removeRecipe(ClientRemoveRecipeParam(param.mealPlanRecipeId))) {
            is Result.Success -> Result.Success(mealPlanId)
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.RecipeNotFound(param.mealPlanRecipeId))
                else -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
            }
        }
    }
}
