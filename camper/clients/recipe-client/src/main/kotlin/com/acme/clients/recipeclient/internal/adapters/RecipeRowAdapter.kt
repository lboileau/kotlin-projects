package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.Recipe
import java.sql.ResultSet
import java.util.UUID

internal object RecipeRowAdapter {
    fun fromResultSet(rs: ResultSet): Recipe = Recipe(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        description = rs.getString("description"),
        webLink = rs.getString("web_link"),
        baseServings = rs.getInt("base_servings"),
        status = rs.getString("status"),
        createdBy = rs.getObject("created_by", UUID::class.java),
        duplicateOfId = rs.getObject("duplicate_of_id", UUID::class.java),
        meal = rs.getString("meal"),
        theme = rs.getString("theme"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
