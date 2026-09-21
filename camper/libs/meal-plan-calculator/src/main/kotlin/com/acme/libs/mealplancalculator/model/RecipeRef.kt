package com.acme.libs.mealplancalculator.model

import java.util.UUID

/** A lightweight reference to a recipe that contributed to a shopping list row. */
data class RecipeRef(
    val id: UUID,
    val name: String,
)
