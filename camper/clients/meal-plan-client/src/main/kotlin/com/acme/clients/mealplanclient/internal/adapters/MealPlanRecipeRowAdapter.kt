package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.MealPlanRecipe
import java.sql.ResultSet
import java.util.UUID

internal object MealPlanRecipeRowAdapter {
    fun fromResultSet(rs: ResultSet): MealPlanRecipe = MealPlanRecipe(
        id = rs.getObject("id", UUID::class.java),
        mealPlanDayId = rs.getObject("meal_plan_day_id", UUID::class.java),
        mealType = rs.getString("meal_type"),
        recipeId = rs.getObject("recipe_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
