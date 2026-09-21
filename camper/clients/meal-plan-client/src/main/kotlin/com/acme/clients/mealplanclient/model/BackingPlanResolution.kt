package com.acme.clients.mealplanclient.model

import java.util.UUID

/**
 * The result of [getOrCreateBackingPlan][com.acme.clients.mealplanclient.api.MealPlanClient.getOrCreateBackingPlan]:
 * the backing plan's id (the share token), and whether that plan was actually created by the
 * share flow (`isRealTrip = false`) versus an existing real camping trip the meal plan happens to
 * be bound to (`isRealTrip = true`). The service layer uses this to refuse issuing a share link
 * for a real trip — see `docs/meal-app-frontend/sharing.md`.
 */
data class BackingPlanResolution(
    val planId: UUID,
    val isRealTrip: Boolean,
)
