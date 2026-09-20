package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.CopyToTripParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateCopyToTrip
import com.acme.clients.mealplanclient.api.AddDayParam as ClientAddDayParam
import com.acme.clients.mealplanclient.api.AddRecipeParam as ClientAddRecipeParam
import com.acme.clients.mealplanclient.api.CreateMealPlanParam as ClientCreateMealPlanParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

/**
 * Copies a template to a trip. Authorizes both sides: the SOURCE template (owner/member of it,
 * via [MealPlanAuthorizer]) and the TARGET trip ([CopyToTripParam.planId], via
 * [PlanRoleAuthorizer]) — without the latter, anyone could copy a template into an arbitrary
 * trip UUID, claiming its meal-plan slot and exposing its owner/members via `GET /members`.
 */
internal class CopyToTripAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val validate = ValidateCopyToTrip()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    fun execute(param: CopyToTripParam): Result<MealPlanDetailResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        when (val access = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        val targetAuthResult = planRoleAuthorizer.authorize(
            param.planId, param.userId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (targetAuthResult is Result.Failure) {
            return Result.Failure(MealPlanError.PlanAccessDenied(param.planId, param.userId))
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
                        mealPlanId = newMealPlan.id,
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

        return MealPlanDetailBuilder.buildDetail(newMealPlan, param.userId, mealPlanClient, recipeClient, ingredientClient)
    }
}
