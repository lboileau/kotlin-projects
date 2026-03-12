package com.acme.services.camperservice.features.mealplan.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class MealPlanResponse(
    val id: UUID,
    val planId: UUID?,
    val name: String,
    val servings: Int,
    val scalingMode: String,
    val isTemplate: Boolean,
    val sourceTemplateId: UUID?,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class MealPlanDetailResponse(
    val id: UUID,
    val planId: UUID?,
    val name: String,
    val servings: Int,
    val scalingMode: String,
    val isTemplate: Boolean,
    val sourceTemplateId: UUID?,
    val createdBy: UUID,
    val days: List<MealPlanDayResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class MealPlanDayResponse(
    val id: UUID,
    val dayNumber: Int,
    val meals: MealsByTypeResponse
)

data class MealsByTypeResponse(
    val breakfast: List<MealPlanRecipeDetailResponse>,
    val lunch: List<MealPlanRecipeDetailResponse>,
    val dinner: List<MealPlanRecipeDetailResponse>,
    val snack: List<MealPlanRecipeDetailResponse>
)

data class MealPlanRecipeDetailResponse(
    val id: UUID,
    val recipeId: UUID,
    val recipeName: String,
    val baseServings: Int,
    val scaleFactor: BigDecimal,
    val isFullyPurchased: Boolean,
    val ingredients: List<MealPlanIngredientResponse>
)

data class MealPlanIngredientResponse(
    val recipeIngredientId: UUID,
    val ingredientId: UUID,
    val ingredientName: String,
    val category: String,
    val quantity: BigDecimal,
    val scaledQuantity: BigDecimal,
    val unit: String
)

data class ShoppingListResponse(
    val mealPlanId: UUID,
    val servings: Int,
    val scalingMode: String,
    val totalItems: Int,
    val fullyPurchasedCount: Int,
    val categories: List<ShoppingListCategoryResponse>
)

data class ShoppingListCategoryResponse(
    val category: String,
    val items: List<ShoppingListItemResponse>
)

data class ShoppingListItemResponse(
    val ingredientId: UUID,
    val ingredientName: String,
    val quantityRequired: BigDecimal,
    val quantityPurchased: BigDecimal,
    val unit: String,
    val status: String,
    val usedInRecipes: List<String>
)

data class ShoppingListPurchaseResponse(
    val id: UUID,
    val mealPlanId: UUID,
    val ingredientId: UUID,
    val unit: String,
    val quantityPurchased: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant
)
