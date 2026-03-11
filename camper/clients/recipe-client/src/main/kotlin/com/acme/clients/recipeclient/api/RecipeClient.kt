package com.acme.clients.recipeclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient

/**
 * Client interface for Recipe and RecipeIngredient entity operations.
 *
 * Recipes represent cooking recipes with an associated list of ingredients.
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface RecipeClient {
    /** Create a new recipe. */
    fun create(param: CreateRecipeParam): Result<Recipe, AppError>

    /** Retrieve a recipe by its unique identifier. */
    fun getById(param: GetByIdParam): Result<Recipe, AppError>

    /** Retrieve all recipes, optionally filtered by status or creator. */
    fun getAll(param: GetAllParam): Result<List<Recipe>, AppError>

    /** Update an existing recipe. Null fields are left unchanged. */
    fun update(param: UpdateRecipeParam): Result<Recipe, AppError>

    /** Delete a recipe by its unique identifier. */
    fun delete(param: DeleteRecipeParam): Result<Unit, AppError>

    /** Find a recipe by its source web URL. Returns null if not found. */
    fun findByWebLink(param: FindByWebLinkParam): Result<Recipe?, AppError>

    /** Find recipes with a name similar to the given query (for duplicate detection). */
    fun findSimilarByName(param: FindSimilarParam): Result<List<Recipe>, AppError>

    /** Add a single ingredient entry to a recipe. */
    fun addIngredient(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError>

    /** Add multiple ingredient entries to a recipe in a single operation. */
    fun addIngredients(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError>

    /** Retrieve all ingredient entries for a recipe. */
    fun getIngredients(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError>

    /** Update a recipe ingredient entry. Null fields are left unchanged. */
    fun updateIngredient(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError>

    /** Remove a recipe ingredient entry by its unique identifier. */
    fun removeIngredient(param: RemoveRecipeIngredientParam): Result<Unit, AppError>

    /** Find all recipe ingredient entries that reference a given global ingredient. */
    fun findIngredientsByIngredientId(param: FindRecipeIngredientsByIngredientIdParam): Result<List<RecipeIngredient>, AppError>
}
