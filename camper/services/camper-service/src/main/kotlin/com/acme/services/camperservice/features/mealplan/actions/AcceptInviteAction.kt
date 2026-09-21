package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.AddMealPlanMemberParam
import com.acme.clients.mealplanclient.api.GetByPlanIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.AcceptInviteResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.AcceptInviteParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateAcceptInvite
import java.util.UUID

/**
 * Anyone signed in: resolves a share token — the backing trip plan's id — and joins its meal
 * plan. Only honours the token when its plan was actually created by the share flow (see
 * `BackingPlanMarker` in meal-plan-client); a real trip's id whose meal plan the caller learned
 * some other way is treated as an unknown token (`404`), so this can't be used to self-join a
 * real trip. Idempotent — accepting an already-accepted invite (or the owner opening their own
 * link) is a no-op that reports `alreadyMember = true`. The controller publishes a "members
 * updated" event only when this call actually inserted a new membership (`!alreadyMember`).
 */
internal class AcceptInviteAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateAcceptInvite()

    fun execute(param: AcceptInviteParam): Result<AcceptInviteResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val planId = try {
            UUID.fromString(param.token)
        } catch (e: IllegalArgumentException) {
            return Result.Failure(MealPlanError.InviteNotFound(param.token))
        }

        val mealPlan = when (val result = mealPlanClient.getShareBackingMealPlan(GetByPlanIdParam(planId))) {
            is Result.Success -> result.value ?: return Result.Failure(MealPlanError.InviteNotFound(param.token))
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
        }

        if (mealPlan.createdBy == param.userId) {
            return Result.Success(
                AcceptInviteResponse(mealPlanId = mealPlan.id, name = mealPlan.name, role = "owner", alreadyMember = true)
            )
        }

        val inserted = when (val result = mealPlanClient.addMember(AddMealPlanMemberParam(mealPlan.id, param.userId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("member", result.error.message))
        }

        return Result.Success(
            AcceptInviteResponse(mealPlanId = mealPlan.id, name = mealPlan.name, role = "member", alreadyMember = !inserted)
        )
    }
}
