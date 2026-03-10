package com.acme.services.camperservice.features.recipe.mapper

import com.acme.clients.ingredientclient.model.Ingredient as ClientIngredient
import com.acme.clients.recipeclient.model.Recipe as ClientRecipe
import com.acme.clients.recipeclient.model.RecipeIngredient as ClientRecipeIngredient
import com.acme.services.camperservice.features.recipe.dto.IngredientResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
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

    fun toRecipeResponse(recipe: ClientRecipe): RecipeResponse = RecipeResponse(
        id = recipe.id,
        name = recipe.name,
        description = recipe.description,
        webLink = recipe.webLink,
        baseServings = recipe.baseServings,
        status = recipe.status,
        createdBy = recipe.createdBy,
        duplicateOfId = recipe.duplicateOfId,
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
        reviewFlags = recipeIngredient.reviewFlags,
        createdAt = recipeIngredient.createdAt,
        updatedAt = recipeIngredient.updatedAt
    )

    fun toRecipeDetailResponse(
        recipe: ClientRecipe,
        duplicateOf: RecipeResponse?,
        ingredients: List<RecipeIngredientResponse>
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
        createdAt = recipe.createdAt,
        updatedAt = recipe.updatedAt
    )
}
