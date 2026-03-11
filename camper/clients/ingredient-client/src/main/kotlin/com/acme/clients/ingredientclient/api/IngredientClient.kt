package com.acme.clients.ingredientclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.ingredientclient.model.Ingredient

/**
 * Client interface for Ingredient entity operations.
 *
 * Ingredients are global master records used for normalizing recipe ingredients
 * across all recipes. All operations return [Result] to represent success or
 * typed failure without throwing exceptions for expected error conditions.
 */
interface IngredientClient {
    /** Create a new global ingredient. */
    fun create(param: CreateIngredientParam): Result<Ingredient, AppError>

    /** Retrieve an ingredient by its unique identifier. */
    fun getById(param: GetByIdParam): Result<Ingredient, AppError>

    /** Retrieve all global ingredients, ordered by name. */
    fun getAll(): Result<List<Ingredient>, AppError>

    /** Update an existing ingredient. Null fields are left unchanged. */
    fun update(param: UpdateIngredientParam): Result<Ingredient, AppError>

    /** Delete an ingredient by its unique identifier. */
    fun delete(param: DeleteIngredientParam): Result<Unit, AppError>

    /** Find a single ingredient by name (case-insensitive). Returns null if not found. */
    fun findByName(param: FindByNameParam): Result<Ingredient?, AppError>

    /** Find ingredients by a set of names (case-insensitive). Returns only matching records. */
    fun findByNames(param: FindByNamesParam): Result<List<Ingredient>, AppError>

    /** Create multiple ingredients in a single operation. */
    fun createBatch(param: CreateBatchParam): Result<List<Ingredient>, AppError>
}
