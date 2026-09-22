package com.acme.services.camperservice.features.recipe.mapper

import com.acme.clients.ingredientclient.model.Ingredient as ClientIngredient
import com.acme.clients.recipeclient.model.Recipe as ClientRecipe
import com.acme.clients.recipeclient.model.RecipeFavorite as ClientRecipeFavorite
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary as ClientRecipeFavoriteSummary
import com.acme.clients.recipeclient.model.RecipeIngredient as ClientRecipeIngredient
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.dto.RecipePhotoResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteUserResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeIngredientResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse

object RecipeMapper {

    fun toIngredientResponse(ingredient: ClientIngredient): IngredientResponse = IngredientResponse(
        id = ingredient.id,
        name = ingredient.name,
        category = ingredient.category,
        defaultUnit = ingredient.defaultUnit,
        createdAt = ingredient.createdAt,
        updatedAt = ingredient.updatedAt
    )

    /**
     * [favoriteCount] and [favoritedByMe] are deliberately required, with no defaults:
     * a caller that forgot to read the favourite summary would otherwise silently
     * return `0 / false` for a recipe that already has favourites.
     */
    fun toRecipeResponse(
        recipe: ClientRecipe,
        favoriteCount: Int,
        favoritedByMe: Boolean
    ): RecipeResponse = RecipeResponse(
        id = recipe.id,
        name = recipe.name,
        description = recipe.description,
        webLink = recipe.webLink,
        baseServings = recipe.baseServings,
        status = recipe.status,
        createdBy = recipe.createdBy,
        duplicateOfId = recipe.duplicateOfId,
        meal = recipe.meal,
        theme = recipe.theme,
        favoriteCount = favoriteCount,
        favoritedByMe = favoritedByMe,
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt
    )

    fun toRecipeIngredientResponse(
        recipeIngredient: ClientRecipeIngredient,
        ingredient: IngredientResponse?,
        matchedIngredient: IngredientResponse?
    ): RecipeIngredientResponse = RecipeIngredientResponse(
        id = recipeIngredient.id,
        recipeId = recipeIngredient.recipeId,
        ingredient = ingredient,
        originalText = recipeIngredient.originalText,
        quantity = recipeIngredient.quantity,
        unit = recipeIngredient.unit,
        status = recipeIngredient.status,
        matchedIngredient = matchedIngredient,
        suggestedIngredientName = recipeIngredient.suggestedIngredientName,
        suggestedCategory = recipeIngredient.suggestedCategory,
        suggestedUnit = recipeIngredient.suggestedUnit,
        reviewFlags = recipeIngredient.reviewFlags,
        createdAt = recipeIngredient.createdAt,
        updatedAt = recipeIngredient.updatedAt
    )

    /**
     * [favoriteCount] and [favoritedByMe] are required for the same reason as in [toRecipeResponse];
     * so are [steps] and [photos] — a caller that forgot to load them would otherwise ship a
     * recipe that looks like it has none.
     */
    fun toRecipeDetailResponse(
        recipe: ClientRecipe,
        duplicateOf: RecipeResponse?,
        ingredients: List<RecipeIngredientResponse>,
        favoriteCount: Int,
        favoritedByMe: Boolean,
        steps: List<String>,
        photos: List<RecipePhotoResponse>
    ): RecipeDetailResponse = RecipeDetailResponse(
        id = recipe.id,
        name = recipe.name,
        description = recipe.description,
        webLink = recipe.webLink,
        baseServings = recipe.baseServings,
        status = recipe.status,
        createdBy = recipe.createdBy,
        duplicateOf = duplicateOf,
        ingredients = ingredients,
        meal = recipe.meal,
        theme = recipe.theme,
        favoriteCount = favoriteCount,
        favoritedByMe = favoritedByMe,
        steps = steps,
        photos = photos,
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt
    )

    fun toFavoriteStatusResponse(summary: ClientRecipeFavoriteSummary): RecipeFavoriteStatusResponse =
        RecipeFavoriteStatusResponse(
            recipeId = summary.recipeId,
            favoriteCount = summary.favoriteCount,
            favoritedByMe = summary.favoritedByMe
        )

    fun toFavoriteUserResponse(
        favorite: ClientRecipeFavorite,
        username: String
    ): RecipeFavoriteUserResponse = RecipeFavoriteUserResponse(
        userId = favorite.userId,
        username = username,
        favoritedAt = favorite.createdAt
    )
}
