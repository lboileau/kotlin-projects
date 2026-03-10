package com.acme.services.camperservice.features.recipe.dto

import java.math.BigDecimal
import java.util.UUID

data class CreateRecipeRequest(
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val ingredients: List<CreateRecipeIngredientRequest>
)

data class CreateRecipeIngredientRequest(
    val ingredientId: UUID,
    val quantity: BigDecimal,
    val unit: String
)

data class ImportRecipeRequest(val url: String)

data class UpdateRecipeRequest(val name: String?, val description: String?, val baseServings: Int?)

data class ResolveIngredientRequest(
    val action: String,
    val ingredientId: UUID?,
    val newIngredient: CreateIngredientRequest?
)

data class ResolveDuplicateRequest(val action: String)
