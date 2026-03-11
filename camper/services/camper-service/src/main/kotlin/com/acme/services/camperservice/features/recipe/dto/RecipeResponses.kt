package com.acme.services.camperservice.features.recipe.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class RecipeResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val duplicateOfId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RecipeDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val duplicateOf: RecipeResponse?,
    val ingredients: List<RecipeIngredientResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RecipeIngredientResponse(
    val id: UUID,
    val recipeId: UUID,
    val ingredient: IngredientResponse?,
    val originalText: String?,
    val quantity: BigDecimal,
    val unit: String,
    val status: String,
    val matchedIngredient: IngredientResponse?,
    val suggestedIngredientName: String?,
    val reviewFlags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
