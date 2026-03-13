package com.acme.clients.mealplanclient.model

import java.time.Instant
import java.util.UUID

data class MealPlan(
    val id: UUID,
    val planId: UUID?,
    val name: String,
    val servings: Int,
    val scalingMode: String,
    val isTemplate: Boolean,
    val sourceTemplateId: UUID?,
    val createdBy: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
)
