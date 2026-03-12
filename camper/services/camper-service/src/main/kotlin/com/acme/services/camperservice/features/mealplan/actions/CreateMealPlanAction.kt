package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.CreateMealPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateCreateMealPlan
import com.acme.clients.mealplanclient.api.CreateMealPlanParam as ClientCreateMealPlanParam

internal class CreateMealPlanAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateCreateMealPlan()

    fun execute(param: CreateMealPlanParam): Result<MealPlanResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val mealPlan = when (val result = mealPlanClient.create(
            ClientCreateMealPlanParam(
                planId = param.planId,
                name = param.name,
                servings = param.servings,
                scalingMode = param.scalingMode ?: "fractional",
                isTemplate = param.isTemplate ?: false,
                sourceTemplateId = null,
                createdBy = param.userId,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is ConflictError -> Result.Failure(MealPlanError.PlanAlreadyHasMealPlan(param.planId!!))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        return Result.Success(MealPlanMapper.toMealPlanResponse(mealPlan))
    }
}
