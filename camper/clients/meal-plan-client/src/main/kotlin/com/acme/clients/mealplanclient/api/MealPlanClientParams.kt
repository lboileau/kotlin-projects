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

/** Parameter for retrieving all meal plans created by a given user. */
data class GetByCreatedByParam(val createdBy: UUID)

/** Parameter for retrieving meal plans a user owns plus meal plans shared with them. */
data class GetMineParam(val userId: UUID)

/** Parameter for resolving a user's access to a meal plan. */
data class GetAccessParam(
    val mealPlanId: UUID,
    val userId: UUID,
)

/** Parameter for updating an existing meal plan. Null fields are left unchanged. */
data class UpdateMealPlanParam(
    val id: UUID,
    val name: String? = null,
    val servings: Int? = null,
    val scalingMode: String? = null,
)

/** Parameter for deleting a meal plan by its unique identifier. */
data class DeleteMealPlanParam(val id: UUID)

/** Parameter for duplicating a meal plan (name, servings, scaling mode, days, and recipes). Purchases and manual items are not copied. */
data class DuplicateMealPlanParam(
    val sourceMealPlanId: UUID,
    val name: String,
    val createdBy: UUID,
)

// --- Day params ---

/** Parameter for adding a day to a meal plan. */
data class AddDayParam(
    val mealPlanId: UUID,
    val dayNumber: Int,
)

/** Parameter for retrieving all days for a meal plan. */
data class GetDaysParam(val mealPlanId: UUID)

/** Parameter for removing a day from a meal plan. Scoped to mealPlanId — a day belonging to a different meal plan is treated as not found. */
data class RemoveDayParam(
    val mealPlanId: UUID,
    val id: UUID,
)

// --- Recipe params ---

/** Parameter for adding a recipe to a meal on a day. Scoped to mealPlanId — mealPlanDayId must belong to it, else the day is treated as not found. */
data class AddRecipeParam(
    val mealPlanId: UUID,
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

/** Parameter for resolving the owning meal plan ID for a meal plan recipe entry. */
data class GetMealPlanIdForRecipeParam(val mealPlanRecipeId: UUID)

/** Parameter for removing every occurrence of a recipe from a meal plan (all days, all meal types) in one call. */
data class RemoveRecipeFromPlanParam(
    val mealPlanId: UUID,
    val recipeId: UUID,
)

/**
 * Parameter for atomically finding-or-creating a meal_plan_recipes row for (mealPlanId, recipeId).
 * The whole check-and-insert (including finding/creating the lowest-numbered day) runs under a
 * lock on the meal plan row, so concurrent calls for the same plan cannot double-insert the recipe.
 */
data class AddRecipeToPlanIfAbsentParam(
    val mealPlanId: UUID,
    val recipeId: UUID,
)

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

// --- Shopping list manual item params ---

/** Parameter for adding a manual item to a shopping list. */
data class AddManualItemParam(
    val mealPlanId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
)

/** Parameter for retrieving all manual items for a meal plan. */
data class GetManualItemsParam(val mealPlanId: UUID)

/** Parameter for removing a manual item by ID. Scoped to mealPlanId — an item belonging to a different meal plan is treated as not found. */
data class RemoveManualItemParam(
    val mealPlanId: UUID,
    val id: UUID,
)

/** Parameter for updating the purchased quantity of a manual item. Scoped to mealPlanId — an item belonging to a different meal plan is treated as not found. */
data class UpdateManualItemPurchaseParam(
    val mealPlanId: UUID,
    val id: UUID,
    val quantityPurchased: BigDecimal,
)

/** Parameter for resetting all manual item purchases for a meal plan. */
data class ResetManualItemPurchasesParam(val mealPlanId: UUID)

// --- Sharing & member params ---

/** Parameter for lazily creating or retrieving a meal plan's backing plan (share token). */
data class GetShareTokenParam(val mealPlanId: UUID)

/** Parameter for adding a member to a meal plan. */
data class AddMealPlanMemberParam(
    val mealPlanId: UUID,
    val userId: UUID,
)

/** Parameter for retrieving all members of a meal plan. */
data class GetMealPlanMembersParam(val mealPlanId: UUID)

/** Parameter for removing a member from a meal plan. */
data class RemoveMealPlanMemberParam(
    val mealPlanId: UUID,
    val userId: UUID,
)
