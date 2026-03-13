package com.acme.libs.mealplancalculator.model

import java.math.BigDecimal
import java.util.UUID

data class ScaledIngredient(
    val ingredientId: UUID,
    val recipeIngredientId: UUID,
    val recipeName: String,
    val quantity: BigDecimal,
    val unit: String,
)
