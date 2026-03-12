package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.SaveAsTemplateParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateSaveAsTemplate
import com.acme.clients.mealplanclient.api.AddDayParam as ClientAddDayParam
import com.acme.clients.mealplanclient.api.AddRecipeParam as ClientAddRecipeParam
import com.acme.clients.mealplanclient.api.CreateMealPlanParam as ClientCreateMealPlanParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

internal class SaveAsTemplateAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateSaveAsTemplate()

    fun execute(param: SaveAsTemplateParam): Result<MealPlanResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Load source meal plan — must NOT be a template
        val source = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        if (source.isTemplate) {
            return Result.Failure(MealPlanError.IsATemplate(source.id))
        }

        // Create new template meal plan
        val template = when (val result = mealPlanClient.create(
            ClientCreateMealPlanParam(
                planId = null,
                name = param.name,
                servings = source.servings,
                scalingMode = source.scalingMode,
                isTemplate = true,
                sourceTemplateId = null,
                createdBy = param.userId,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
        }

        // Copy days and recipes (no purchases)
        val sourceDays = when (val result = mealPlanClient.getDays(GetDaysParam(source.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("days", result.error.message))
        }

        for (day in sourceDays) {
            val newDay = when (val result = mealPlanClient.addDay(ClientAddDayParam(template.id, day.dayNumber))) {
                is Result.Success -> result.value
                is Result.Failure -> return Result.Failure(MealPlanError.Invalid("day", result.error.message))
            }

            val recipes = when (val result = mealPlanClient.getRecipesByDayId(GetRecipesByDayIdParam(day.id))) {
                is Result.Success -> result.value
                is Result.Failure -> return Result.Failure(MealPlanError.Invalid("recipes", result.error.message))
            }

            for (recipe in recipes) {
                when (val result = mealPlanClient.addRecipe(
                    ClientAddRecipeParam(
                        mealPlanDayId = newDay.id,
                        mealType = recipe.mealType,
                        recipeId = recipe.recipeId,
                    )
                )) {
                    is Result.Success -> {}
                    is Result.Failure -> return Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
                }
            }
        }

        return Result.Success(MealPlanMapper.toMealPlanResponse(template))
    }
}
