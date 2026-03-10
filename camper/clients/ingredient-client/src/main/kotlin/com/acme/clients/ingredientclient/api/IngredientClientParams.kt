package com.acme.clients.ingredientclient.api

import java.util.UUID

/** Parameter for creating a new ingredient. */
data class CreateIngredientParam(
    val name: String,
    val category: String,
    val defaultUnit: String
)

/** Parameter for retrieving an ingredient by its unique identifier. */
data class GetByIdParam(val id: UUID)

/** Parameter for updating an existing ingredient. Null fields are left unchanged. */
data class UpdateIngredientParam(
    val id: UUID,
    val name: String? = null,
    val category: String? = null,
    val defaultUnit: String? = null
)

/** Parameter for finding an ingredient by name (case-insensitive). */
data class FindByNameParam(val name: String)

/** Parameter for finding ingredients by a set of names (case-insensitive). */
data class FindByNamesParam(val names: List<String>)

/** Parameter for creating multiple ingredients in a single operation. */
data class CreateBatchParam(val ingredients: List<CreateIngredientParam>)
