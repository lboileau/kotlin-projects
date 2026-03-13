package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetMealPlanDetailParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetMealPlanDetail
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

internal class GetMealPlanDetailAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateGetMealPlanDetail()

    fun execute(param: GetMealPlanDetailParam): Result<MealPlanDetailResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val mealPlan = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        return MealPlanDetailBuilder.buildDetail(mealPlan, mealPlanClient, recipeClient, ingredientClient)
    }
}
