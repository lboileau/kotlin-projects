package com.acme.clients.mealplanclient.model

import java.time.Instant
import java.util.UUID

/** A single person with access to a meal plan via its backing plan (owner_id or plan_members), excluding the meal plan's own owner. Username/email enrichment happens at the service layer. */
data class MealPlanMember(
    val userId: UUID,
    val createdAt: Instant,
)
