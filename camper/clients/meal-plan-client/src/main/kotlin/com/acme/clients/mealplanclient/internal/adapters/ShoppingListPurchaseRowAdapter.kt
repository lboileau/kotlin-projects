package com.acme.clients.mealplanclient.internal.adapters

import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import java.sql.ResultSet
import java.util.UUID

internal object ShoppingListPurchaseRowAdapter {
    fun fromResultSet(rs: ResultSet): ShoppingListPurchase = ShoppingListPurchase(
        id = rs.getObject("id", UUID::class.java),
        mealPlanId = rs.getObject("meal_plan_id", UUID::class.java),
        ingredientId = rs.getObject("ingredient_id", UUID::class.java),
        unit = rs.getString("unit"),
        quantityPurchased = rs.getBigDecimal("quantity_purchased"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
