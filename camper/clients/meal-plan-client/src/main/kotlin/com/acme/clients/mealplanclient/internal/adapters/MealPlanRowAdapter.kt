package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.MealPlan
import java.sql.ResultSet
import java.util.UUID

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
    )
}
