package com.acme.clients.recipeclient.model

import java.time.Instant
import java.util.UUID

/** One step of a recipe's instructions. Steps are only ever read and replaced as a whole list. */
data class RecipeStep(
    val id: UUID,
    val recipeId: UUID,
    val position: Int,
    val text: String,
    val createdAt: Instant
)
