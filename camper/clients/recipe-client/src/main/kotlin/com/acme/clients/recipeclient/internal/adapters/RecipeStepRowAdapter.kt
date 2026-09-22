package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.RecipeStep
import java.sql.ResultSet
import java.util.UUID

internal object RecipeStepRowAdapter {
    const val COLUMNS = "id, recipe_id, position, text, created_at"

    fun fromResultSet(rs: ResultSet): RecipeStep = RecipeStep(
        id = rs.getObject("id", UUID::class.java),
        recipeId = rs.getObject("recipe_id", UUID::class.java),
        position = rs.getInt("position"),
        text = rs.getString("text"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
