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
    val createdBy: UUID
)

/** Parameter for retrieving a recipe by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for retrieving all recipes, with optional status filter. */
data class GetAllParam(
    val status: String? = null,
    val createdBy: UUID? = null
)

/** Parameter for updating an existing recipe. Null fields are left unchanged. */
data class UpdateRecipeParam(
    val id: UUID,
    val name: String? = null,
    val description: String? = null,
    val baseServings: Int? = null,
    val status: String? = null,
    val duplicateOfId: UUID? = null,
    val clearDuplicateOf: Boolean = false
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
