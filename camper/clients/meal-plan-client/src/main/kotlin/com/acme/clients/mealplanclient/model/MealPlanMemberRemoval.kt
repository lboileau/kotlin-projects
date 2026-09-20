package com.acme.clients.mealplanclient.model

/** The outcome of attempting to remove a user from a meal plan's backing plan. */
enum class MealPlanMemberRemoval {
    /** A plan_members row was deleted. */
    REMOVED,

    /** The user wasn't a member (idempotent — nothing to do). */
    NOT_A_MEMBER,

    /** The user is the backing plan's owner_id, who cannot be removed via plan_members. */
    IS_BACKING_PLAN_OWNER,
}
