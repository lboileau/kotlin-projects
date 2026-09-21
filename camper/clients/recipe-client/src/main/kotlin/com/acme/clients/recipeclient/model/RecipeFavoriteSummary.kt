package com.acme.clients.recipeclient.model

import java.util.UUID

/**
 * Per-recipe favourite totals for one caller. Produced by a single batched
 * read across many recipe ids — never one query per recipe.
 */
data class RecipeFavoriteSummary(
    val recipeId: UUID,
    val favoriteCount: Int,
    val favoritedByMe: Boolean
)
