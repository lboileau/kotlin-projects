package com.acme.clients.mealplanclient.model

import java.time.Instant
import java.util.UUID

data class MealPlanRecipe(
    val id: UUID,
    val mealPlanDayId: UUID,
    val mealType: String,
    val recipeId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
)
