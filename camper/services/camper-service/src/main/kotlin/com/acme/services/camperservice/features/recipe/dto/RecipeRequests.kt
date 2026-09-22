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
    val ingredients: List<CreateRecipeIngredientRequest>,
    /** The method as ordered step texts; absent or empty means none. */
    val steps: List<String>? = null
)

data class CreateRecipeIngredientRequest(
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String
)

data class ImportRecipeRequest(val url: String)

/**
 * Photos of one recipe: the one showing the ingredient list (`role = "ingredients"`) and, optionally,
 * the one showing the method (`role = "instructions"`). A single photo with no role is read as a
 * whole. Both are attached to the draft as its photos.
 */
data class ImportRecipeFromImagesRequest(val images: List<ImportImageRequest>)

/**
 * `mediaType` is image/jpeg, image/png, image/gif or image/webp; `data` is raw base64 (no `data:`
 * prefix); `role` is `ingredients`, `instructions` or absent.
 */
data class ImportImageRequest(val mediaType: String, val data: String, val role: String? = null)

/** Body of PUT /api/recipes/{id}/steps — the whole list; an empty list clears the steps. */
data class ReplaceRecipeStepsRequest(val steps: List<String>)

/** Body of POST /api/recipes/{id}/photos: same shape as an import image, no role. */
data class AddRecipePhotoRequest(val mediaType: String, val data: String)

data class UpdateRecipeRequest(val name: String?, val description: String?, val baseServings: Int?, val meal: String? = null, val theme: String? = null)

data class ResolveIngredientRequest(
    val action: String,
    val ingredientId: UUID?,
    val newIngredient: CreateIngredientRequest?,
    val quantity: BigDecimal?,
    val unit: String?
)

data class ResolveDuplicateRequest(val action: String)
