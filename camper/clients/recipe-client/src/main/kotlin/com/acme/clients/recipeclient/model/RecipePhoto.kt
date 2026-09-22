package com.acme.clients.recipeclient.model

import java.time.Instant
import java.util.UUID

/**
 * A photo attached to a recipe — metadata only; the bytes are an object at [storageKey] in the
 * photo store. [source] is how it got here (`upload` by a person, `import` = what an import was
 * read from), [role] which part of the recipe an import photo showed (`ingredients` /
 * `instructions`), null for uploads.
 */
data class RecipePhoto(
    val id: UUID,
    val recipeId: UUID,
    val position: Int,
    val storageKey: String,
    val mediaType: String,
    val byteSize: Int,
    val width: Int?,
    val height: Int?,
    val source: String,
    val role: String?,
    val createdBy: UUID,
    val createdAt: Instant
) {
    companion object {
        const val SOURCE_UPLOAD = "upload"
        const val SOURCE_IMPORT = "import"
        const val ROLE_INGREDIENTS = "ingredients"
        const val ROLE_INSTRUCTIONS = "instructions"
        val SOURCES = setOf(SOURCE_UPLOAD, SOURCE_IMPORT)
        val ROLES = setOf(ROLE_INGREDIENTS, ROLE_INSTRUCTIONS)
    }
}
