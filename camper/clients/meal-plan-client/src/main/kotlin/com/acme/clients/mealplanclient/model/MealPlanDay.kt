package com.acme.clients.mealplanclient.model

import java.time.Instant
import java.util.UUID

data class MealPlanDay(
    val id: UUID,
    val mealPlanId: UUID,
    val dayNumber: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
