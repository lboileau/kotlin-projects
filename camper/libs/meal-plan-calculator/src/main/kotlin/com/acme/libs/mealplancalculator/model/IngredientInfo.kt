package com.acme.libs.mealplancalculator.model

import java.util.UUID

data class IngredientInfo(
    val ingredientId: UUID,
    val name: String,
    val category: String,
)
