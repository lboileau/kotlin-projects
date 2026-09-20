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
    /** Number of distinct recipes across all days of this plan. */
    val recipeCount: Int,
    /** Number of distinct people with access to this plan via its backing plan (does not include the owner). */
    val memberCount: Int = 0,
    /** The owner's (createdBy's) username, falling back to email. Empty if unresolved. */
    val ownerName: String = "",
)
