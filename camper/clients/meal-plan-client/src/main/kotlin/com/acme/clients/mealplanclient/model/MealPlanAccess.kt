package com.acme.clients.mealplanclient.model

import java.util.UUID

/**
 * The information needed to resolve a caller's role on a meal plan: who owns it, and whether the
 * caller has access via its backing plan (the backing plan's owner_id or a row in its
 * plan_members). Resolved in a single statement.
 */
data class MealPlanAccess(
    val createdBy: UUID,
    val isMember: Boolean,
)
