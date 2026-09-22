package com.acme.services.camperservice.features.recipe.dto

import java.math.BigDecimal
import java.util.UUID

data class CreateRecipeRequest(
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val meal: String? = null,
    val theme: String? = null,
    val ingredients: List<CreateRecipeIngredientRequest>
)

data class CreateRecipeIngredientRequest(
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String
)

data class ImportRecipeRequest(val url: String)

/** Up to three photos of one recipe, in reading order. */
data class ImportRecipeFromImagesRequest(val images: List<ImportImageRequest>)

/** `mediaType` is image/jpeg, image/png, image/gif or image/webp; `data` is raw base64 (no `data:` prefix). */
data class ImportImageRequest(val mediaType: String, val data: String)

data class UpdateRecipeRequest(val name: String?, val description: String?, val baseServings: Int?, val meal: String? = null, val theme: String? = null)

data class ResolveIngredientRequest(
    val action: String,
    val ingredientId: UUID?,
    val newIngredient: CreateIngredientRequest?,
    val quantity: BigDecimal?,
    val unit: String?
)

data class ResolveDuplicateRequest(val action: String)
