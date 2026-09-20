package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.mealplanclient.api.GetShareTokenParam as ClientGetShareTokenParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.ShareTokenResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetShareTokenParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetShareToken

/**
 * Owner or member: lazily creates the meal plan's share token on first call. If the meal plan is
 * already bound to a real trip (not a plan created by this share flow), refuses with `409` rather
 * than handing out a token that would let a joiner into the whole trip.
 */
internal class GetShareTokenAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateGetShareToken()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    fun execute(param: GetShareTokenParam): Result<ShareTokenResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        when (val access = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        return when (val result = mealPlanClient.getOrCreateBackingPlan(ClientGetShareTokenParam(param.mealPlanId))) {
            is Result.Success -> if (result.value.isRealTrip) {
                Result.Failure(MealPlanError.PlanBoundToRealTrip(param.mealPlanId))
            } else {
                Result.Success(ShareTokenResponse(result.value.planId.toString()))
            }
            is Result.Failure -> Result.Failure(MealPlanError.Invalid("shareToken", result.error.message))
        }
    }
}
