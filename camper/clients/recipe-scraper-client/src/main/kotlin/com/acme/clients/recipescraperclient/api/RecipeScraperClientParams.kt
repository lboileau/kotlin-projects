package com.acme.clients.recipescraperclient.api

import java.util.UUID

data class ScrapeRecipeParam(
    val html: String,
    val sourceUrl: String,
    val existingIngredients: List<ExistingIngredient>
)

/**
 * One or more photos of the same recipe (a cookbook page, a recipe card, a handwritten note, a
 * screenshot), in reading order. The caller has already checked the count, media types and sizes.
 */
data class ScrapeRecipeImagesParam(
    val images: List<RecipeImage>,
    val existingIngredients: List<ExistingIngredient>
)

/** A photo as the model receives it: a supported media type and the bytes already base64-encoded. */
data class RecipeImage(
    val mediaType: String,
    val base64Data: String
) {
    companion object {
        /** What the Claude API accepts as an image block. */
        val SUPPORTED_MEDIA_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        const val MAX_IMAGES = 3
    }
}

data class ExistingIngredient(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String
)
