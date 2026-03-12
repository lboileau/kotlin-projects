package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.MealPlanDay
import java.sql.ResultSet
import java.util.UUID

internal object MealPlanDayRowAdapter {
    fun fromResultSet(rs: ResultSet): MealPlanDay = MealPlanDay(
        id = rs.getObject("id", UUID::class.java),
        mealPlanId = rs.getObject("meal_plan_id", UUID::class.java),
        dayNumber = rs.getInt("day_number"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
