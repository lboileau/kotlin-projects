package com.acme.clients.ingredientclient.model

import java.time.Instant
import java.util.UUID

data class Ingredient(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
