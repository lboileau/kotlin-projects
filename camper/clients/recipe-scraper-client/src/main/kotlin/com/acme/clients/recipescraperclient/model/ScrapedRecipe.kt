package com.acme.clients.recipescraperclient.model

import java.math.BigDecimal
import java.util.UUID

data class ScrapedRecipe(
    val name: String,
    val description: String?,
    val baseServings: Int,
    val ingredients: List<ScrapedIngredient>
)

data class ScrapedIngredient(
    val originalText: String,
    val quantity: BigDecimal,
    val unit: String,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String?,
    val confidence: String,
    val reviewFlags: List<String>
)
