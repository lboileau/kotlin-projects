package com.acme.clients.ingredientclient.internal.adapters

import com.acme.clients.ingredientclient.model.Ingredient
import java.sql.ResultSet
import java.util.UUID

internal object IngredientRowAdapter {
    fun fromResultSet(rs: ResultSet): Ingredient = Ingredient(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        category = rs.getString("category"),
        defaultUnit = rs.getString("default_unit"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
