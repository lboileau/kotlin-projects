package com.acme.services.camperservice.features.mealplan.dto

import java.math.BigDecimal
import java.util.UUID

data class CreateMealPlanRequest(
    val name: String,
    val servings: Int,
    val scalingMode: String?,
    val isTemplate: Boolean?,
    val planId: UUID?
)

data class UpdateMealPlanRequest(
    val name: String?,
    val servings: Int?,
    val scalingMode: String?
)

data class AddDayRequest(
    val dayNumber: Int
)

data class AddRecipeRequest(
    val mealType: String,
    val recipeId: UUID
)

data class CopyToTripRequest(
    val planId: UUID,
    val servings: Int?
)

data class SaveAsTemplateRequest(
    val name: String
)

data class UpdatePurchaseRequest(
    val ingredientId: UUID? = null,
    val manualItemId: UUID? = null,
    val unit: String? = null,
    val quantityPurchased: BigDecimal,
)

data class AddManualItemRequest(
    val ingredientId: UUID? = null,
    val description: String? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
)
