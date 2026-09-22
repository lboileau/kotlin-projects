package com.acme.clients.recipescraperclient.api

import java.util.UUID

data class ScrapeRecipeParam(
    val html: String,
    val sourceUrl: String,
    val existingIngredients: List<ExistingIngredient>
)

/**
 * Photos of one recipe (a cookbook page, a recipe card, a handwritten note, a screenshot): one of
 * the ingredient list and, optionally, one of the method. The caller has already checked the
 * count, roles, media types and sizes.
 */
data class ScrapeRecipeImagesParam(
    val images: List<RecipeImage>,
    val existingIngredients: List<ExistingIngredient>
)

/**
 * A photo as the model receives it: a supported media type, the bytes already base64-encoded, and
 * which part of the recipe it shows so the prompt can say so. A null role is "a photo of the
 * recipe" with no promise about what is on it.
 */
data class RecipeImage(
    val mediaType: String,
    val base64Data: String,
    val role: String? = null
) {
    companion object {
        /** What the Claude API accepts as an image block. */
        val SUPPORTED_MEDIA_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        const val ROLE_INGREDIENTS = "ingredients"
        const val ROLE_INSTRUCTIONS = "instructions"
        val ROLES = setOf(ROLE_INGREDIENTS, ROLE_INSTRUCTIONS)
        /** One per role. */
        const val MAX_IMAGES = 2
    }
}

data class ExistingIngredient(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String
)
