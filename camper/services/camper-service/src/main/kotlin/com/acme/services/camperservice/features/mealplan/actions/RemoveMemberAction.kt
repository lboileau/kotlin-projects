package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.mealplanclient.api.RemoveMealPlanMemberParam as ClientRemoveMealPlanMemberParam
import com.acme.clients.mealplanclient.model.MealPlanMemberRemoval
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.auth.MealPlanRole
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.RemoveMealPlanMemberParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateRemoveMealPlanMember

/**
 * Owner may remove anyone (except the owner); a member may only remove themselves (leave).
 * Removing the meal plan's owner, or the backing trip's owner_id (a legacy trip owner who isn't
 * the meal plan's creator), is `400` — neither can be removed via plan_members. Removing a user
 * who isn't currently a member is idempotent (204) — nothing to do. Returns whether a row was
 * actually removed, so the controller only publishes an event on a real change.
 */
internal class RemoveMemberAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateRemoveMealPlanMember()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    fun execute(param: RemoveMealPlanMemberParam): Result<Boolean, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        val access = when (val result = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }

        if (param.targetUserId == access.ownerId) {
            return Result.Failure(MealPlanError.CannotRemoveOwner(param.mealPlanId))
        }

        if (access.role != MealPlanRole.OWNER && param.targetUserId != param.userId) {
            return Result.Failure(MealPlanError.Forbidden(param.mealPlanId, param.userId))
        }

        return when (val result = mealPlanClient.removeMember(ClientRemoveMealPlanMemberParam(param.mealPlanId, param.targetUserId))) {
            is Result.Success -> when (result.value) {
                MealPlanMemberRemoval.REMOVED -> Result.Success(true)
                MealPlanMemberRemoval.NOT_A_MEMBER -> Result.Success(false)
                MealPlanMemberRemoval.IS_BACKING_PLAN_OWNER -> Result.Failure(MealPlanError.CannotRemoveOwner(param.mealPlanId))
            }
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("member", result.error.message))
        }
    }
}
