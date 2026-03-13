package com.acme.libs.mealplancalculator.model

import java.math.BigDecimal
import java.util.UUID

data class RecipeIngredientWithMeta(
    val recipeIngredientId: UUID,
    val ingredientId: UUID,
    val ingredientName: String,
    val category: String,
    val quantity: BigDecimal,
    val unit: String,
    val recipeName: String,
    val baseServings: Int,
)
