package com.acme.services.camperservice.features.recipe.error

import com.acme.clients.common.error.AppError
import java.util.UUID

sealed class RecipeError(override val message: String) : AppError {
    data class NotFound(val recipeId: UUID) : RecipeError("Recipe not found: $recipeId")
    data class NotCreator(val recipeId: UUID, val userId: UUID) : RecipeError("User $userId is not the creator of recipe $recipeId")
    data class Invalid(val field: String, val reason: String) : RecipeError("Invalid $field: $reason")
    data class DuplicateWebLink(val url: String) : RecipeError("Recipe with URL already exists: $url")
    data class DuplicateIngredientName(val name: String) : RecipeError("Ingredient already exists: $name")
    data class UnresolvedIngredients(val recipeId: UUID, val count: Int) : RecipeError("Recipe $recipeId has $count unresolved ingredients")
    data class UnresolvedDuplicate(val recipeId: UUID) : RecipeError("Recipe $recipeId has an unresolved duplicate flag")
    data class ImportFailed(val url: String, val reason: String) : RecipeError("Import failed for $url: $reason")
    data class ScrapeFailed(val reason: String) : RecipeError("Scrape failed: $reason")
    data class IngredientNotFound(val ingredientId: UUID) : RecipeError("Ingredient not found: $ingredientId")
    data class AlreadyPublished(val recipeId: UUID) : RecipeError("Recipe $recipeId is already published")
}
