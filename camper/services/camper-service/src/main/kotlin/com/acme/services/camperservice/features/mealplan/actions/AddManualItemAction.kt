package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.libs.mealplancalculator.model.PurchaseStatus
import com.acme.services.camperservice.features.mealplan.dto.ManualItemResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.AddManualItemParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateAddManualItem
import java.math.BigDecimal
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam
import com.acme.clients.mealplanclient.api.AddManualItemParam as ClientAddManualItemParam
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam

internal class AddManualItemAction(
    private val mealPlanClient: MealPlanClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateAddManualItem()

    fun execute(param: AddManualItemParam): Result<ManualItemResponse, MealPlanError> {
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

        // Resolve ingredient info if ingredient-based
        var ingredientName: String? = null
        var category: String = "misc"

        if (param.ingredientId != null) {
            when (val result = ingredientClient.getById(IngredientGetByIdParam(param.ingredientId))) {
                is Result.Success -> {
                    ingredientName = result.value.name
                    category = result.value.category
                }
                is Result.Failure -> return when (result.error) {
                    is NotFoundError -> Result.Failure(MealPlanError.Invalid("ingredientId", "ingredient not found: ${param.ingredientId}"))
                    else -> Result.Failure(MealPlanError.Invalid("ingredientId", result.error.message))
                }
            }
        }

        // Convert to client param: free-form items get quantity=1
        val clientParam = ClientAddManualItemParam(
            mealPlanId = param.mealPlanId,
            ingredientId = param.ingredientId,
            description = param.description,
            quantity = param.quantity ?: BigDecimal.ONE,
            unit = param.unit,
        )

        val item = when (val result = mealPlanClient.addManualItem(clientParam)) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is ConflictError -> Result.Failure(MealPlanError.DuplicateManualItem(param.ingredientId!!, param.unit!!))
                else -> Result.Failure(MealPlanError.Invalid("manualItem", result.error.message))
            }
        }

        val status = PurchaseStatus.derive(item.quantity, item.quantityPurchased).name.lowercase()

        return Result.Success(
            ManualItemResponse(
                id = item.id,
                ingredientId = item.ingredientId,
                ingredientName = ingredientName,
                description = item.description,
                quantity = item.quantity,
                unit = item.unit,
                quantityPurchased = item.quantityPurchased,
                status = status,
                category = category,
            )
        )
    }
}
