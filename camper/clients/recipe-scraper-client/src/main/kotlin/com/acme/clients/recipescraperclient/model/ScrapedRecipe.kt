package com.acme.clients.recipescraperclient.model

import java.math.BigDecimal
import java.util.UUID

data class ScrapedRecipe(
    val name: String,
    val description: String?,
    val baseServings: Int,
    val meal: String? = null,
    val theme: String? = null,
    val ingredients: List<ScrapedIngredient>,
    /** The method as ordered step texts; empty when the source had none. */
    val steps: List<String> = emptyList()
)

data class ScrapedIngredient(
    val originalText: String,
    val quantity: BigDecimal,
    val unit: String,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String?,
    val suggestedCategory: String? = null,
    val suggestedUnit: String? = null,
    val confidence: String,
    val reviewFlags: List<String>
)
