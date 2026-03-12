package com.acme.libs.mealplancalculator.model

import java.math.BigDecimal
import java.util.UUID

data class ShoppingListRow(
    val ingredientId: UUID,
    val ingredientName: String,
    val category: String,
    val quantityRequired: BigDecimal,
    val unit: String,
    val usedInRecipes: List<String>,
)
