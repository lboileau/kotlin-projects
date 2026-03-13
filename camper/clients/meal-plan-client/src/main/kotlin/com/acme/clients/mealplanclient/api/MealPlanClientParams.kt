package com.acme.clients.mealplanclient.api

import java.math.BigDecimal
import java.util.UUID

// --- Meal Plan params ---

/** Parameter for creating a new meal plan. */
data class CreateMealPlanParam(
    val planId: UUID?,
    val name: String,
    val servings: Int,
    val scalingMode: String,
    val isTemplate: Boolean,
    val sourceTemplateId: UUID?,
    val createdBy: UUID,
)

/** Parameter for retrieving a meal plan by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving a meal plan by its associated plan (trip) ID. */
data class GetByPlanIdParam(val planId: UUID)

/** Parameter for updating an existing meal plan. Null fields are left unchanged. */
data class UpdateMealPlanParam(
    val id: UUID,
    val name: String? = null,
    val servings: Int? = null,
    val scalingMode: String? = null,
)

/** Parameter for deleting a meal plan by its unique identifier. */
data class DeleteMealPlanParam(val id: UUID)

// --- Day params ---

/** Parameter for adding a day to a meal plan. */
data class AddDayParam(
    val mealPlanId: UUID,
    val dayNumber: Int,
)

/** Parameter for retrieving all days for a meal plan. */
data class GetDaysParam(val mealPlanId: UUID)

/** Parameter for removing a day from a meal plan. */
data class RemoveDayParam(val id: UUID)

// --- Recipe params ---

/** Parameter for adding a recipe to a meal on a day. */
data class AddRecipeParam(
    val mealPlanDayId: UUID,
    val mealType: String,
    val recipeId: UUID,
)

/** Parameter for retrieving all recipes for a specific day. */
data class GetRecipesByDayIdParam(val mealPlanDayId: UUID)

/** Parameter for retrieving all recipes for a meal plan. */
data class GetRecipesByMealPlanIdParam(val mealPlanId: UUID)

/** Parameter for removing a recipe from a meal. */
data class RemoveRecipeParam(val id: UUID)

// --- Shopping list purchase params ---

/** Parameter for retrieving all purchases for a meal plan. */
data class GetPurchasesParam(val mealPlanId: UUID)

/** Parameter for upserting a purchase record for an ingredient/unit. */
data class UpsertPurchaseParam(
    val mealPlanId: UUID,
    val ingredientId: UUID,
    val unit: String,
    val quantityPurchased: BigDecimal,
)

/** Parameter for deleting all purchases for a meal plan. */
data class DeletePurchasesParam(val mealPlanId: UUID)
