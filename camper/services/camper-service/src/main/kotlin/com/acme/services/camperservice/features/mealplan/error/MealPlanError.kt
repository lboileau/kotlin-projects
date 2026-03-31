package com.acme.services.camperservice.features.mealplan.error

import com.acme.clients.common.error.AppError
import java.util.UUID

sealed class MealPlanError(override val message: String) : AppError {
    data class MealPlanNotFound(val id: UUID) : MealPlanError("Meal plan not found: $id")
    data class DayNotFound(val id: UUID) : MealPlanError("Meal plan day not found: $id")
    data class RecipeNotFound(val id: UUID) : MealPlanError("Recipe not found: $id")
    data class DuplicateDayNumber(val dayNumber: Int) : MealPlanError("Duplicate day number: $dayNumber")
    data class PlanAlreadyHasMealPlan(val planId: UUID) : MealPlanError("Plan already has a meal plan: $planId")
    data class NotATemplate(val id: UUID) : MealPlanError("Meal plan is not a template: $id")
    data class IsATemplate(val id: UUID) : MealPlanError("Meal plan is a template: $id")
    data class Invalid(val field: String, val reason: String) : MealPlanError("Invalid $field: $reason")
    data class ManualItemNotFound(val id: UUID) : MealPlanError("Manual item not found: $id")
    data class DuplicateManualItem(val ingredientId: UUID, val unit: String) : MealPlanError("Manual item already exists for ingredient $ingredientId with unit $unit")
}
