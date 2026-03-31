package com.acme.services.camperservice.features.mealplan.params

import java.math.BigDecimal
import java.util.UUID

data class CreateMealPlanParam(
    val userId: UUID,
    val name: String,
    val servings: Int,
    val scalingMode: String?,
    val isTemplate: Boolean?,
    val planId: UUID?
)

data class GetMealPlanDetailParam(
    val mealPlanId: UUID,
    val userId: UUID
)

data class GetMealPlanByPlanIdParam(
    val planId: UUID,
    val userId: UUID
)

data class GetTemplatesParam(
    val userId: UUID
)

data class UpdateMealPlanParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val name: String?,
    val servings: Int?,
    val scalingMode: String?
)

data class DeleteMealPlanParam(
    val mealPlanId: UUID,
    val userId: UUID
)

data class CopyToTripParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val planId: UUID,
    val servings: Int?
)

data class SaveAsTemplateParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val name: String
)

data class AddDayParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val dayNumber: Int
)

data class RemoveDayParam(
    val mealPlanId: UUID,
    val dayId: UUID,
    val userId: UUID
)

data class AddRecipeToMealParam(
    val mealPlanId: UUID,
    val dayId: UUID,
    val userId: UUID,
    val mealType: String,
    val recipeId: UUID
)

data class RemoveRecipeFromMealParam(
    val mealPlanRecipeId: UUID,
    val userId: UUID
)

data class GetShoppingListParam(
    val mealPlanId: UUID,
    val userId: UUID
)

data class UpdatePurchaseParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val ingredientId: UUID?,
    val manualItemId: UUID?,
    val unit: String?,
    val quantityPurchased: BigDecimal
)

data class AddManualItemParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal?,
    val unit: String?,
)

data class RemoveManualItemParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val itemId: UUID,
)

data class ResetPurchasesParam(
    val mealPlanId: UUID,
    val userId: UUID
)
