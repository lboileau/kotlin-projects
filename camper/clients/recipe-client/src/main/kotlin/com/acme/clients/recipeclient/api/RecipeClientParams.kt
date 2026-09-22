package com.acme.clients.recipeclient.api

import java.math.BigDecimal
import java.util.UUID

/** Parameter for creating a new recipe. */
data class CreateRecipeParam(
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val meal: String? = null,
    val theme: String? = null
)

/** Parameter for retrieving a recipe by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving all recipes, with optional status filter. */
data class GetAllParam(
    val status: String? = null,
    val createdBy: UUID? = null
)

/**
 * Parameter for updating an existing recipe. Null fields are left unchanged.
 *
 * [description], [meal], and [theme] can be explicitly cleared to NULL via their
 * respective `clear*` flags (mirroring [clearDuplicateOf]) — the field itself should
 * stay null when clearing; callers should not set both a non-null value and its
 * clear flag at once.
 */
data class UpdateRecipeParam(
    val id: UUID,
    val name: String? = null,
    val description: String? = null,
    val clearDescription: Boolean = false,
    val baseServings: Int? = null,
    val status: String? = null,
    val duplicateOfId: UUID? = null,
    val clearDuplicateOf: Boolean = false,
    val meal: String? = null,
    val clearMeal: Boolean = false,
    val theme: String? = null,
    val clearTheme: Boolean = false
)

/** Parameter for deleting a recipe by its unique identifier. */
data class DeleteRecipeParam(val id: UUID)

/** Parameter for finding a recipe by its source URL. */
data class FindByWebLinkParam(val webLink: String)

/** Parameter for finding recipes with a name similar to the given query. */
data class FindSimilarParam(val name: String)

/** Parameter for adding a single ingredient to a recipe. */
data class AddRecipeIngredientParam(
    val recipeId: UUID,
    val ingredientId: UUID?,
    val originalText: String?,
    val quantity: BigDecimal,
    val unit: String,
    val status: String,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String?,
    val suggestedCategory: String? = null,
    val suggestedUnit: String? = null,
    val reviewFlags: List<String>
)

/** Parameter for adding multiple ingredients to a recipe in a single operation. */
data class AddRecipeIngredientsParam(val ingredients: List<AddRecipeIngredientParam>)

/** Parameter for retrieving all ingredients for a recipe. */
data class GetRecipeIngredientsParam(val recipeId: UUID)

/** Parameter for updating a recipe ingredient. Null fields are left unchanged. */
data class UpdateRecipeIngredientParam(
    val id: UUID,
    val ingredientId: UUID?,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
    val status: String? = null,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String? = null,
    val reviewFlags: List<String>? = null,
    val clearMatchedIngredient: Boolean = false
)

/** Parameter for removing a recipe ingredient by its unique identifier. */
data class RemoveRecipeIngredientParam(val id: UUID)

/** Parameter for finding recipe ingredients that reference a given global ingredient. */
data class FindRecipeIngredientsByIngredientIdParam(val ingredientId: UUID)

/** Parameter for favouriting a recipe on behalf of a user. Idempotent. */
data class AddRecipeFavoriteParam(val recipeId: UUID, val userId: UUID)

/** Parameter for removing a user's favourite of a recipe. Idempotent. */
data class RemoveRecipeFavoriteParam(val recipeId: UUID, val userId: UUID)

/** Parameter for listing who favourited a recipe, oldest first. */
data class GetRecipeFavoritesParam(val recipeId: UUID)

/**
 * Parameter for the batched favourite summary read.
 *
 * [recipeIds] may be empty — the operation then returns an empty list without
 * touching the database (an empty `IN ()` is a SQL syntax error).
 */
data class GetRecipeFavoriteSummariesParam(val recipeIds: List<UUID>, val userId: UUID)

/** Parameter for reading a recipe's steps in order. */
data class GetRecipeStepsParam(val recipeId: UUID)

/**
 * Parameter for replacing a recipe's steps with [texts], in order. An empty list clears them.
 * Every text must be non-blank; positions are assigned from list order.
 */
data class ReplaceRecipeStepsParam(val recipeId: UUID, val texts: List<String>)

/** Parameter for listing a recipe's photos in display order. */
data class GetRecipePhotosParam(val recipeId: UUID)

/** Parameter for reading one photo's metadata by its id. */
data class GetRecipePhotoByIdParam(val id: UUID)

/**
 * Parameter for recording a photo whose bytes are already at [storageKey]. The id is chosen by
 * the caller so the storage key can embed it before the row exists. Position is the next free one.
 */
data class AddRecipePhotoParam(
    val id: UUID,
    val recipeId: UUID,
    val storageKey: String,
    val mediaType: String,
    val byteSize: Int,
    val width: Int?,
    val height: Int?,
    val source: String,
    val role: String?,
    val createdBy: UUID
)

/** Parameter for removing a photo's row. The caller deletes the object first. */
data class RemoveRecipePhotoParam(val id: UUID)
