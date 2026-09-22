package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.model.Ingredient
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import com.acme.clients.recipeclient.api.CreateRecipeParam as ClientCreateRecipeParam
import com.acme.clients.recipeclient.api.FindSimilarParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.acme.services.camperservice.features.recipe.error.RecipeError
import java.util.UUID

/**
 * The part of an import that is the same whatever the recipe came from (a URL, photos): the
 * catalogue the scraper matches against, and turning its answer into a draft recipe with lines
 * awaiting review. Shared by [ImportRecipeAction] and [ImportRecipeFromImagesAction].
 */
internal class ScrapedRecipeDrafter(
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient
) {

    /** Every ingredient, both as the scraper wants it and as the detail response needs it. */
    internal class Catalogue(val all: List<Ingredient>) {
        val existing: List<ExistingIngredient> = all.map { ing ->
            ExistingIngredient(id = ing.id, name = ing.name, category = ing.category, defaultUnit = ing.defaultUnit)
        }
    }

    fun loadCatalogue(): Result<Catalogue, RecipeError> =
        when (val result = ingredientClient.getAll()) {
            is Result.Success -> Result.Success(Catalogue(result.value))
            is Result.Failure -> Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

    /**
     * Creates the draft, flags a similar-named recipe as its possible duplicate, stores every line
     * as `approved` (clean high-confidence match) or `pending_review` (anything flagged), writes
     * the steps, and returns the new recipe's id. The caller reads the detail back through
     * [GetRecipeAction] once it has attached whatever else (photos) belongs on the draft.
     */
    fun createDraft(
        scraped: ScrapedRecipe,
        webLink: String?,
        userId: UUID
    ): Result<UUID, RecipeError> {
        // Check for similar existing recipes (duplicate detection)
        val similarRecipes = when (val result = recipeClient.findSimilarByName(FindSimilarParam(scraped.name))) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }
        val duplicateOfId = similarRecipes.firstOrNull()?.id

        // Create draft recipe
        val recipe = when (val result = recipeClient.create(ClientCreateRecipeParam(
            name = scraped.name,
            description = scraped.description,
            webLink = webLink,
            baseServings = scraped.baseServings,
            status = "draft",
            createdBy = userId,
            meal = scraped.meal,
            theme = scraped.theme
        ))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
        }

        // Set duplicate_of_id if found
        if (duplicateOfId != null) {
            recipeClient.update(ClientUpdateRecipeParam(id = recipe.id, duplicateOfId = duplicateOfId))
        }

        // Add recipe ingredients with review state
        val ingredientParams = scraped.ingredients.map { ing ->
            AddRecipeIngredientParam(
                recipeId = recipe.id,
                ingredientId = if (ing.reviewFlags.isEmpty() && ing.matchedIngredientId != null) ing.matchedIngredientId else null,
                originalText = ing.originalText,
                quantity = ing.quantity,
                unit = ing.unit,
                status = if (ing.reviewFlags.isEmpty() && ing.matchedIngredientId != null) "approved" else "pending_review",
                matchedIngredientId = ing.matchedIngredientId,
                suggestedIngredientName = ing.suggestedIngredientName,
                suggestedCategory = ing.suggestedCategory,
                suggestedUnit = ing.suggestedUnit,
                reviewFlags = ing.reviewFlags
            )
        }
        if (ingredientParams.isNotEmpty()) {
            when (val result = recipeClient.addIngredients(AddRecipeIngredientsParam(ingredientParams))) {
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
                is Result.Success -> {}
            }
        }

        // Steps are stored as read — there is nothing to review in a method.
        val steps = when (val validated = ReplaceRecipeStepsAction.validateSteps(scraped.steps)) {
            is Result.Success -> validated.value
            is Result.Failure -> scraped.steps.map { it.trim() }.filter { it.isNotEmpty() }.take(ReplaceRecipeStepsAction.MAX_STEPS)
        }
        if (steps.isNotEmpty()) {
            when (val result = recipeClient.replaceSteps(ReplaceRecipeStepsParam(recipe.id, steps.map { it.take(ReplaceRecipeStepsAction.MAX_STEP_CHARS) }))) {
                is Result.Failure -> return Result.Failure(RecipeError.Invalid("steps", result.error.message))
                is Result.Success -> {}
            }
        }

        return Result.Success(recipe.id)
    }
}
