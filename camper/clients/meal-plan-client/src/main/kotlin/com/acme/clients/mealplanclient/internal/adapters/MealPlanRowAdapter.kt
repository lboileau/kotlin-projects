package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.MealPlan
import java.sql.ResultSet
import java.util.UUID

/**
 * Shared SELECT fragment for reading a meal_plans row plus its derived, read-time-only columns:
 * `recipe_count` (distinct recipes across all days), `member_count` (distinct people with access
 * via the backing trip plan — its `owner_id` plus its `plan_members` — not counting the meal
 * plan's own owner), and `owner_name` (the owner's username, falling back to email). A meal plan
 * with no backing plan (`plan_id IS NULL`) naturally has `member_count = 0`, since both branches
 * of the union match nothing. Used by every operation that returns a [MealPlan] so every read
 * gets these for free.
 */
internal object MealPlanSelect {
    const val COLUMNS = """
        id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at,
        (SELECT COUNT(DISTINCT mpr.recipe_id) FROM meal_plan_recipes mpr JOIN meal_plan_days d ON d.id = mpr.meal_plan_day_id WHERE d.meal_plan_id = meal_plans.id) AS recipe_count,
        (SELECT COUNT(DISTINCT person_id) FROM (
            SELECT p.owner_id AS person_id FROM plans p WHERE p.id = meal_plans.plan_id
            UNION
            SELECT pm.user_id AS person_id FROM plan_members pm WHERE pm.plan_id = meal_plans.plan_id
        ) people WHERE person_id <> meal_plans.created_by) AS member_count,
        (SELECT COALESCE(u.username, u.email) FROM users u WHERE u.id = meal_plans.created_by) AS owner_name
    """
}

internal object MealPlanRowAdapter {
    fun fromResultSet(rs: ResultSet): MealPlan = MealPlan(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        name = rs.getString("name"),
        servings = rs.getInt("servings"),
        scalingMode = rs.getString("scaling_mode"),
        isTemplate = rs.getBoolean("is_template"),
        sourceTemplateId = rs.getObject("source_template_id", UUID::class.java),
        createdBy = rs.getObject("created_by", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        recipeCount = rs.getInt("recipe_count"),
        memberCount = rs.getInt("member_count"),
        ownerName = rs.getString("owner_name") ?: "",
    )
}
