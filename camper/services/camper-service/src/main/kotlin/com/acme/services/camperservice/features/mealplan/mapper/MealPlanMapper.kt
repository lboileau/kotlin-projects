package com.acme.services.camperservice.features.mealplan.mapper

import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDayResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.dto.MealsByTypeResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanRecipeDetailResponse
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListPurchaseResponse
import java.util.UUID

object MealPlanMapper {

    /** "owner" if the caller created the plan, else "member" (assumes access was already authorized). */
    fun roleFor(mealPlan: MealPlan, userId: UUID): String = if (mealPlan.createdBy == userId) "owner" else "member"

    fun toMealPlanResponse(mealPlan: MealPlan, userId: UUID): MealPlanResponse = MealPlanResponse(
        id = mealPlan.id,
        planId = mealPlan.planId,
        name = mealPlan.name,
        servings = mealPlan.servings,
        scalingMode = mealPlan.scalingMode,
        isTemplate = mealPlan.isTemplate,
        sourceTemplateId = mealPlan.sourceTemplateId,
        createdBy = mealPlan.createdBy,
        createdAt = mealPlan.createdAt,
        updatedAt = mealPlan.updatedAt,
        recipeCount = mealPlan.recipeCount,
        role = roleFor(mealPlan, userId),
        memberCount = mealPlan.memberCount,
        ownerName = mealPlan.ownerName,
    )

    fun toMealPlanDayResponse(
        day: MealPlanDay,
        recipesByMealType: Map<String, List<MealPlanRecipeDetailResponse>>,
    ): MealPlanDayResponse = MealPlanDayResponse(
        id = day.id,
        dayNumber = day.dayNumber,
        meals = MealsByTypeResponse(
            breakfast = recipesByMealType["breakfast"] ?: emptyList(),
            lunch = recipesByMealType["lunch"] ?: emptyList(),
            dinner = recipesByMealType["dinner"] ?: emptyList(),
            snack = recipesByMealType["snack"] ?: emptyList(),
        ),
    )

    fun toShoppingListPurchaseResponse(purchase: ShoppingListPurchase): ShoppingListPurchaseResponse =
        ShoppingListPurchaseResponse(
            id = purchase.id,
            mealPlanId = purchase.mealPlanId,
            ingredientId = purchase.ingredientId,
            unit = purchase.unit,
            quantityPurchased = purchase.quantityPurchased,
            createdAt = purchase.createdAt,
            updatedAt = purchase.updatedAt,
        )
}
