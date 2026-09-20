package com.acme.clients.mealplanclient.internal

import java.util.UUID

/**
 * Distinguishes a `plans` row created BY the meal-plan sharing flow (a "backing plan") from a
 * real camping trip that a meal plan happens to be bound to via `meal_plans.plan_id`. There's no
 * spare column for this (no migrations allowed), so the marker is the backing plan's `name`:
 * `"meal-plan-share:<mealPlanId>"`. A real trip's name is whatever its owner chose and will not
 * collide with this format in practice.
 *
 * This matters because `meal_plans.plan_id` doesn't only mean "shared via link" — it can also
 * mean "bound to an existing real trip" (created through the ordinary trip-planning flow). Only
 * the former should honour a share token for joining; the latter must not let a stranger who
 * merely learns the trip's id join the whole trip via the meal-plan invite endpoint.
 */
internal object BackingPlanMarker {
    const val PREFIX = "meal-plan-share:"

    fun nameFor(mealPlanId: UUID): String = "$PREFIX$mealPlanId"

    fun isShareBacking(planName: String?, mealPlanId: UUID): Boolean = planName == nameFor(mealPlanId)
}
