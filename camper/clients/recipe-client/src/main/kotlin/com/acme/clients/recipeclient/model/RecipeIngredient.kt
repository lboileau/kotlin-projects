package com.acme.clients.recipeclient.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class RecipeIngredient(
    val id: UUID,
    val recipeId: UUID,
    val ingredientId: UUID?,
    val originalText: String?,
    val quantity: BigDecimal,
    val unit: String,
    val status: String,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String?,
    val reviewFlags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
