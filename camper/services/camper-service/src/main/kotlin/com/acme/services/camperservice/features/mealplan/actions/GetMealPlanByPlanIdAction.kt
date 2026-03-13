package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.libs.mealplancalculator.ShoppingListCalculator
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetMealPlanByPlanIdParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetMealPlanByPlanId
import com.acme.clients.mealplanclient.api.GetByPlanIdParam as ClientGetByPlanIdParam

internal class GetMealPlanByPlanIdAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateGetMealPlanByPlanId()

    fun execute(param: GetMealPlanByPlanIdParam): Result<MealPlanDetailResponse?, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val mealPlan = when (val result = mealPlanClient.getByPlanId(ClientGetByPlanIdParam(param.planId))) {
            is Result.Success -> result.value ?: return Result.Success(null)
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
        }

        return MealPlanDetailBuilder.buildDetail(mealPlan, mealPlanClient, recipeClient, ingredientClient)
    }
}
