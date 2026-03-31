package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListPurchaseResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import com.acme.services.camperservice.features.mealplan.params.UpdatePurchaseParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateUpdatePurchase
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.mealplanclient.api.UpdateManualItemPurchaseParam as ClientUpdateManualItemPurchaseParam
import com.acme.clients.mealplanclient.api.UpsertPurchaseParam as ClientUpsertPurchaseParam

internal class UpdatePurchaseAction(
    private val mealPlanClient: MealPlanClient,
) {
    private val validate = ValidateUpdatePurchase()

    fun execute(param: UpdatePurchaseParam): Result<ShoppingListPurchaseResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Verify meal plan exists
        when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> {}
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        if (param.manualItemId != null) {
            // Manual item purchase update
            val item = when (val result = mealPlanClient.updateManualItemPurchase(
                ClientUpdateManualItemPurchaseParam(
                    id = param.manualItemId,
                    quantityPurchased = param.quantityPurchased,
                )
            )) {
                is Result.Success -> result.value
                is Result.Failure -> return when (result.error) {
                    is NotFoundError -> Result.Failure(MealPlanError.ManualItemNotFound(param.manualItemId))
                    else -> Result.Failure(MealPlanError.Invalid("manualItem", result.error.message))
                }
            }

            return Result.Success(
                ShoppingListPurchaseResponse(
                    id = item.id,
                    mealPlanId = item.mealPlanId,
                    ingredientId = item.ingredientId ?: item.id,
                    unit = item.unit ?: "",
                    quantityPurchased = item.quantityPurchased,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                )
            )
        } else {
            // Recipe-based purchase update (existing flow)
            val purchase = when (val result = mealPlanClient.upsertPurchase(
                ClientUpsertPurchaseParam(
                    mealPlanId = param.mealPlanId,
                    ingredientId = param.ingredientId!!,
                    unit = param.unit!!,
                    quantityPurchased = param.quantityPurchased,
                )
            )) {
                is Result.Success -> result.value
                is Result.Failure -> return Result.Failure(MealPlanError.Invalid("purchase", result.error.message))
            }

            return Result.Success(MealPlanMapper.toShoppingListPurchaseResponse(purchase))
        }
    }
}
