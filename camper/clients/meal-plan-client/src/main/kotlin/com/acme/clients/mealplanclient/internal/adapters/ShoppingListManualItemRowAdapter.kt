package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import java.sql.ResultSet
import java.util.UUID

internal object ShoppingListManualItemRowAdapter {
    fun fromResultSet(rs: ResultSet): ShoppingListManualItem = ShoppingListManualItem(
        id = rs.getObject("id", UUID::class.java),
        mealPlanId = rs.getObject("meal_plan_id", UUID::class.java),
        ingredientId = rs.getObject("ingredient_id", UUID::class.java),
        description = rs.getString("description"),
        quantity = rs.getBigDecimal("quantity"),
        unit = rs.getString("unit"),
        quantityPurchased = rs.getBigDecimal("quantity_purchased"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
