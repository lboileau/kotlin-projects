package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.CreateMealPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateCreateMealPlan
import com.acme.clients.mealplanclient.api.CreateMealPlanParam as ClientCreateMealPlanParam

/**
 * Creates a meal plan. When [CreateMealPlanParam.planId] is non-null, the caller must belong to
 * that trip (owner, manager, or member) — otherwise anyone could bind a meal plan to an arbitrary
 * trip UUID, claiming that trip's single meal-plan slot and exposing its owner/members as
 * "members" of the attacker's plan via `GET /members`. A standalone meal plan (`planId == null`)
 * has no target trip to check.
 */
internal class CreateMealPlanAction(
    private val mealPlanClient: MealPlanClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val validate = ValidateCreateMealPlan()

    fun execute(param: CreateMealPlanParam): Result<MealPlanResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        if (param.planId != null) {
            val authResult = planRoleAuthorizer.authorize(
                param.planId, param.userId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
            )
            if (authResult is Result.Failure) {
                return Result.Failure(MealPlanError.PlanAccessDenied(param.planId, param.userId))
            }
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

        return Result.Success(MealPlanMapper.toMealPlanResponse(mealPlan, param.userId))
    }
}
