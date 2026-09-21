package com.acme.services.camperservice.features.mealplan.auth

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.GetAccessParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import java.util.UUID

enum class MealPlanRole { OWNER, MEMBER }

/** The result of a successful authorization check: who asked, for what plan, in what role, and who owns it. */
data class MealPlanAccessContext(
    val mealPlanId: UUID,
    val userId: UUID,
    val role: MealPlanRole,
    val ownerId: UUID,
)

/**
 * Resolves a caller's role on a meal plan and checks it against the meal plan's private-access
 * rule: only the owner (`meal_plans.created_by`) or someone with access via its backing trip plan
 * (that plan's `owner_id`, or a row in its `plan_members`) may use it. A missing plan surfaces as
 * [MealPlanError.MealPlanNotFound] (404); anyone else gets [MealPlanError.Forbidden] (403).
 * Modelled on [com.acme.services.camperservice.common.auth.PlanRoleAuthorizer].
 */
class MealPlanAuthorizer(private val mealPlanClient: MealPlanClient) {

    /** Authorize a user as owner or member of a meal plan. */
    fun authorize(mealPlanId: UUID, userId: UUID): Result<MealPlanAccessContext, MealPlanError> {
        val access = when (val result = mealPlanClient.getAccess(GetAccessParam(mealPlanId, userId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        val role = when {
            access.createdBy == userId -> MealPlanRole.OWNER
            access.isMember -> MealPlanRole.MEMBER
            else -> null
        }

        return if (role != null) {
            Result.Success(MealPlanAccessContext(mealPlanId, userId, role, access.createdBy))
        } else {
            Result.Failure(MealPlanError.Forbidden(mealPlanId, userId))
        }
    }

    /** Authorize a user as the meal plan's owner specifically. */
    fun authorizeOwner(mealPlanId: UUID, userId: UUID): Result<MealPlanAccessContext, MealPlanError> =
        when (val result = authorize(mealPlanId, userId)) {
            is Result.Success -> if (result.value.role == MealPlanRole.OWNER) {
                result
            } else {
                Result.Failure(MealPlanError.Forbidden(mealPlanId, userId))
            }
            is Result.Failure -> result
        }
}
