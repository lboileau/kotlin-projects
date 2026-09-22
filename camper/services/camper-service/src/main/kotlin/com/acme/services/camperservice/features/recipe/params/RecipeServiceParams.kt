package com.acme.services.camperservice.features.recipe.params

import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import java.math.BigDecimal
import java.util.UUID

data class CreateIngredientParam(
    val userId: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String
)

data class DeleteIngredientParam(
    val ingredientId: UUID,
    val userId: UUID
)

data class ListIngredientsParam(val userId: UUID)

data class UpdateIngredientParam(
    val ingredientId: UUID,
    val userId: UUID,
    val name: String?,
    val category: String?,
    val defaultUnit: String?
)

data class CreateRecipeIngredientParam(
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String
)

data class CreateRecipeParam(
    val userId: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val meal: String? = null,
    val theme: String? = null,
    val ingredients: List<CreateRecipeIngredientParam>,
    val steps: List<String> = emptyList()
)

data class ImportRecipeParam(
    val userId: UUID,
    val url: String
)

data class ImportRecipeFromImagesParam(
    val userId: UUID,
    val images: List<ImportImageParam>
)

/** One photo: its media type, the bytes as raw base64 (no `data:` prefix), and what it shows. */
data class ImportImageParam(
    val mediaType: String,
    val data: String,
    val role: String? = null
)

data class ReplaceRecipeStepsParam(
    val recipeId: UUID,
    val userId: UUID,
    val steps: List<String>
)

data class AddRecipePhotoParam(
    val recipeId: UUID,
    val userId: UUID,
    val mediaType: String,
    val data: String
)

data class RemoveRecipePhotoParam(
    val recipeId: UUID,
    val photoId: UUID,
    val userId: UUID
)

/** A stored object by its storage key, for the local store's proxy route. */
data class GetStoredPhotoParam(val key: String)

data class GetRecipeParam(
    val recipeId: UUID,
    val userId: UUID
)

data class ListRecipesParam(val userId: UUID)

data class UpdateRecipeParam(
    val recipeId: UUID,
    val userId: UUID,
    val name: String?,
    val description: String?,
    val baseServings: Int?,
    val meal: String? = null,
    val theme: String? = null
)

data class DeleteRecipeParam(
    val recipeId: UUID,
    val userId: UUID
)

data class ResolveIngredientParam(
    val recipeId: UUID,
    val recipeIngredientId: UUID,
    val userId: UUID,
    val action: String,
    val ingredientId: UUID?,
    val newIngredient: CreateIngredientRequest?,
    val quantity: BigDecimal?,
    val unit: String?
)

data class ResolveDuplicateParam(
    val recipeId: UUID,
    val userId: UUID,
    val action: String
)

data class PublishRecipeParam(
    val recipeId: UUID,
    val userId: UUID
)

data class RemoveRecipeIngredientParam(
    val recipeId: UUID,
    val recipeIngredientId: UUID,
    val userId: UUID
)

data class AddRecipeIngredientParam(
    val recipeId: UUID,
    val userId: UUID,
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String
)

data class FavoriteRecipeParam(
    val recipeId: UUID,
    val userId: UUID
)

data class UnfavoriteRecipeParam(
    val recipeId: UUID,
    val userId: UUID
)

data class ListRecipeFavoritesParam(
    val recipeId: UUID,
    val userId: UUID
)
