package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.mealplanclient.api.GetMealPlanMembersParam as ClientGetMealPlanMembersParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.MealPlanMemberResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetMealPlanMembersParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetMealPlanMembers

/**
 * Owner or member: lists the meal plan's members, owner first (joinedAt = plan createdAt), then
 * members ordered by join time. Enriched with username (falling back to email) at the service
 * layer via UserClient — mirrors GetPlanMembersAction, which does the same for plan members.
 */
internal class GetMembersAction(
    private val mealPlanClient: MealPlanClient,
    private val userClient: UserClient,
) {
    private val validate = ValidateGetMealPlanMembers()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    fun execute(param: GetMealPlanMembersParam): Result<List<MealPlanMemberResponse>, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        when (val access = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        val mealPlan = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
        }

        val members = when (val result = mealPlanClient.getMembers(ClientGetMealPlanMembersParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("members", result.error.message))
        }

        val ownerResponse = MealPlanMemberResponse(
            userId = mealPlan.createdBy,
            username = displayName(mealPlan.createdBy),
            role = "owner",
            joinedAt = mealPlan.createdAt,
        )

        val memberResponses = members.map { member ->
            MealPlanMemberResponse(
                userId = member.userId,
                username = displayName(member.userId),
                role = "member",
                joinedAt = member.createdAt,
            )
        }

        return Result.Success(listOf(ownerResponse) + memberResponses)
    }

    private fun displayName(userId: java.util.UUID): String =
        when (val result = userClient.getById(UserGetByIdParam(userId))) {
            is Result.Success -> result.value.username ?: result.value.email
            is Result.Failure -> userId.toString()
        }
}
