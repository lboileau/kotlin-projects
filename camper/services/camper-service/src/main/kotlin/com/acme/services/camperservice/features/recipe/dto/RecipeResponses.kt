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
    val meal: String?,
    val theme: String?,
    val favoriteCount: Int,
    val favoritedByMe: Boolean,
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
    val meal: String?,
    val theme: String?,
    val favoriteCount: Int,
    val favoritedByMe: Boolean,
    /** The method, in order. Empty when the recipe has none. */
    val steps: List<String>,
    /** In display order. Empty when the recipe has none. */
    val photos: List<RecipePhotoResponse>,
    val createdAt: Instant,
    val updatedAt: Instant
)

/** A photo on a recipe. [url] is loadable by a browser with no headers; it is good for at least an hour. */
data class RecipePhotoResponse(
    val id: UUID,
    val url: String,
    val mediaType: String,
    val width: Int?,
    val height: Int?,
    val byteSize: Int,
    val source: String,
    val role: String?,
    val position: Int,
    val createdAt: Instant
)

/** Answer to PUT /api/recipes/{id}/steps. */
data class RecipeStepsResponse(val steps: List<String>)

/** Answer to PUT/DELETE /api/recipes/{id}/favorite. */
data class RecipeFavoriteStatusResponse(
    val recipeId: UUID,
    val favoriteCount: Int,
    val favoritedByMe: Boolean
)

/** One row of GET /api/recipes/{id}/favorites. `username` falls back to email. */
data class RecipeFavoriteUserResponse(
    val userId: UUID,
    val username: String,
    val favoritedAt: Instant
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
    val suggestedCategory: String?,
    val suggestedUnit: String?,
    val reviewFlags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)
