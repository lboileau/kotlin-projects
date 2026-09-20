package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.DuplicateMealPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateDuplicateMealPlan
import com.acme.clients.mealplanclient.api.DuplicateMealPlanParam as ClientDuplicateMealPlanParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

private const val MAX_NAME_LENGTH = 255

/**
 * Duplicates a meal plan: copies name (defaulting to "<source name> copy", truncated to fit the
 * column limit), servings, and scaling mode; the copy is never a template and isn't bound to a
 * trip. Days and recipes are copied (day numbers and meal types preserved); purchases and manual
 * shopping items are not. No sync event is published — the new plan has no subscribers yet.
 */
internal class DuplicateMealPlanAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateDuplicateMealPlan()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    fun execute(param: DuplicateMealPlanParam): Result<MealPlanResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        when (val access = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        val source = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        val name = param.name?.takeIf { it.isNotBlank() } ?: "${source.name} copy".take(MAX_NAME_LENGTH)

        return when (val result = mealPlanClient.duplicate(
            ClientDuplicateMealPlanParam(sourceMealPlanId = source.id, name = name, createdBy = param.userId)
        )) {
            is Result.Success -> Result.Success(MealPlanMapper.toMealPlanResponse(result.value, param.userId))
            is Result.Failure -> when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }
    }
}
