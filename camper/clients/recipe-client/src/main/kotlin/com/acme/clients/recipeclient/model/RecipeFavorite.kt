package com.acme.clients.recipeclient.model

import java.time.Instant
import java.util.UUID

data class RecipeFavorite(
    val id: UUID,
    val recipeId: UUID,
    val userId: UUID,
    val createdAt: Instant
)
