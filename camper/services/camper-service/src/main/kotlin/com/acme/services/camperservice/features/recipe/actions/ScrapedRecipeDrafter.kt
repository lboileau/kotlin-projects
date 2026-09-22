package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.model.Ingredient
import com.acme.clients.recipeclient.api.AddRecipeIngredientParam
import com.acme.clients.recipeclient.api.AddRecipeIngredientsParam
import com.acme.clients.recipeclient.api.CreateRecipeParam as ClientCreateRecipeParam
import com.acme.clients.recipeclient.api.FindSimilarParam
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.UpdateRecipeParam as ClientUpdateRecipeParam
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
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
     * as `approved` (clean high-confidence match) or `pending_review` (anything flagged), and reads
     * the whole thing back as the detail response.
     */
    fun createDraft(
        scraped: ScrapedRecipe,
        webLink: String?,
        userId: UUID,
        catalogue: Catalogue
    ): Result<RecipeDetailResponse, RecipeError> {
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
        val finalRecipe = if (duplicateOfId != null) {
            when (val result = recipeClient.update(ClientUpdateRecipeParam(
                id = recipe.id,
                duplicateOfId = duplicateOfId
            ))) {
                is Result.Success -> result.value
                is Result.Failure -> recipe
            }
        } else {
            recipe
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

        // Fetch full detail for response
        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("ingredients", result.error.message))
        }

        val ingredientMap = catalogue.all.associateBy({ it.id }, { RecipeMapper.toIngredientResponse(it) })

        // The imported draft really is 0 / false, but its nested duplicateOf may already have
        // favourites. One batched read covers both, keeping the same shape as GetRecipeAction.
        val summaries = when (val result = recipeClient.getFavoriteSummaries(
            GetRecipeFavoriteSummariesParam(
                recipeIds = listOfNotNull(finalRecipe.id, finalRecipe.duplicateOfId),
                userId = userId
            )
        )) {
            is Result.Success -> result.value.associateBy { it.recipeId }
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        val duplicateOf = finalRecipe.duplicateOfId?.let { dupId ->
            when (val result = recipeClient.getById(GetByIdParam(dupId))) {
                is Result.Success -> RecipeMapper.toRecipeResponse(
                    result.value,
                    summaries[dupId]?.favoriteCount ?: 0,
                    summaries[dupId]?.favoritedByMe ?: false
                )
                is Result.Failure -> null
            }
        }

        val ingredientResponses = recipeIngredients.map { ri ->
            RecipeMapper.toRecipeIngredientResponse(
                recipeIngredient = ri,
                ingredient = ri.ingredientId?.let { ingredientMap[it] },
                matchedIngredient = ri.matchedIngredientId?.let { ingredientMap[it] }
            )
        }

        return Result.Success(
            RecipeMapper.toRecipeDetailResponse(
                finalRecipe,
                duplicateOf,
                ingredientResponses,
                summaries[finalRecipe.id]?.favoriteCount ?: 0,
                summaries[finalRecipe.id]?.favoritedByMe ?: false
            )
        )
    }
}
