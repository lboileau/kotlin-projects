package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.CopyToTripParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateCopyToTrip
import com.acme.clients.mealplanclient.api.AddDayParam as ClientAddDayParam
import com.acme.clients.mealplanclient.api.AddRecipeParam as ClientAddRecipeParam
import com.acme.clients.mealplanclient.api.CreateMealPlanParam as ClientCreateMealPlanParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

internal class CopyToTripAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateCopyToTrip()

    fun execute(param: CopyToTripParam): Result<MealPlanDetailResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Load source meal plan — must be a template
        val source = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        if (!source.isTemplate) {
            return Result.Failure(MealPlanError.NotATemplate(source.id))
        }

        // Create new meal plan bound to the trip
        val newMealPlan = when (val result = mealPlanClient.create(
            ClientCreateMealPlanParam(
                planId = param.planId,
                name = source.name,
                servings = param.servings ?: source.servings,
                scalingMode = source.scalingMode,
                isTemplate = false,
                sourceTemplateId = source.id,
                createdBy = param.userId,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is ConflictError -> Result.Failure(MealPlanError.PlanAlreadyHasMealPlan(param.planId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        // Copy days and recipes
        val sourceDays = when (val result = mealPlanClient.getDays(GetDaysParam(source.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("days", result.error.message))
        }

        for (day in sourceDays) {
            val newDay = when (val result = mealPlanClient.addDay(ClientAddDayParam(newMealPlan.id, day.dayNumber))) {
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

        return MealPlanDetailBuilder.buildDetail(newMealPlan, mealPlanClient, recipeClient, ingredientClient)
    }
}
