package com.acme.services.camperservice.features.recipe.dto

import java.time.Instant
import java.util.UUID

data class IngredientResponse(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
