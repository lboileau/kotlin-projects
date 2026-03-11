package com.acme.clients.recipescraperclient.api

import java.util.UUID

data class ScrapeRecipeParam(
    val html: String,
    val sourceUrl: String,
    val existingIngredients: List<ExistingIngredient>
)

data class ExistingIngredient(
    val id: UUID,
    val name: String,
    val category: String,
    val defaultUnit: String
)
