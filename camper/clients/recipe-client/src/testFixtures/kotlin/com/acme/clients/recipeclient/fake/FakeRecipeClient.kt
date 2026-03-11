package com.acme.clients.recipeclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.*
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredient
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredients
import com.acme.clients.recipeclient.internal.validations.ValidateCreateRecipe
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipe
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipeIngredient
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeRecipeClient : RecipeClient {
    private val recipes = ConcurrentHashMap<UUID, Recipe>()
    private val ingredients = ConcurrentHashMap<UUID, RecipeIngredient>()

    private val validateCreate = ValidateCreateRecipe()
    private val validateUpdate = ValidateUpdateRecipe()
    private val validateAddIngredient = ValidateAddRecipeIngredient()
    private val validateAddIngredients = ValidateAddRecipeIngredients()
    private val validateUpdateIngredient = ValidateUpdateRecipeIngredient()

    override fun create(param: CreateRecipeParam): Result<Recipe, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.webLink != null && recipes.values.any { it.webLink == param.webLink }) {
            return failure(ConflictError("Recipe", "web_link '${param.webLink}' already exists"))
        }

        val entity = Recipe(
            id = UUID.randomUUID(),
            name = param.name,
            description = param.description,
            webLink = param.webLink,
            baseServings = param.baseServings,
            status = param.status,
            createdBy = param.createdBy,
            duplicateOfId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        recipes[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<Recipe, AppError> {
        val entity = recipes[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("Recipe", param.id.toString()))
    }

    override fun getAll(param: GetAllParam): Result<List<Recipe>, AppError> {
        var result = recipes.values.toList()
        if (param.status != null) result = result.filter { it.status == param.status }
        if (param.createdBy != null) result = result.filter { it.createdBy == param.createdBy }
        return success(result.sortedBy { it.name })
    }

    override fun update(param: UpdateRecipeParam): Result<Recipe, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = recipes[param.id] ?: return failure(NotFoundError("Recipe", param.id.toString()))
        val updated = existing.copy(
            name = param.name ?: existing.name,
            description = param.description ?: existing.description,
            baseServings = param.baseServings ?: existing.baseServings,
            status = param.status ?: existing.status,
            duplicateOfId = when {
                param.clearDuplicateOf -> null
                param.duplicateOfId != null -> param.duplicateOfId
                else -> existing.duplicateOfId
            },
            updatedAt = Instant.now()
        )
        recipes[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteRecipeParam): Result<Unit, AppError> {
        if (!recipes.containsKey(param.id)) return failure(NotFoundError("Recipe", param.id.toString()))
        recipes.remove(param.id)
        ingredients.values.removeIf { it.recipeId == param.id }
        return success(Unit)
    }

    override fun findByWebLink(param: FindByWebLinkParam): Result<Recipe?, AppError> {
        return success(recipes.values.find { it.webLink == param.webLink })
    }

    override fun findSimilarByName(param: FindSimilarParam): Result<List<Recipe>, AppError> {
        val lower = param.name.lowercase()
        return success(recipes.values.filter { it.name.lowercase().contains(lower) }.sortedBy { it.name })
    }

    override fun addIngredient(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        val validation = validateAddIngredient.execute(param)
        if (validation is Result.Failure) return validation

        val entity = RecipeIngredient(
            id = UUID.randomUUID(),
            recipeId = param.recipeId,
            ingredientId = param.ingredientId,
            originalText = param.originalText,
            quantity = param.quantity,
            unit = param.unit,
            status = param.status,
            matchedIngredientId = param.matchedIngredientId,
            suggestedIngredientName = param.suggestedIngredientName,
            reviewFlags = param.reviewFlags,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        ingredients[entity.id] = entity
        return success(entity)
    }

    override fun addIngredients(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        val validation = validateAddIngredients.execute(param)
        if (validation is Result.Failure) return validation

        if (param.ingredients.isEmpty()) return success(emptyList())

        val results = mutableListOf<RecipeIngredient>()
        for (ing in param.ingredients) {
            val result = addIngredient(ing)
            if (result is Result.Failure) return result
            results.add((result as Result.Success).value)
        }
        return success(results)
    }

    override fun getIngredients(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        return success(ingredients.values.filter { it.recipeId == param.recipeId }.sortedBy { it.createdAt })
    }

    override fun updateIngredient(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        val validation = validateUpdateIngredient.execute(param)
        if (validation is Result.Failure) return validation

        val existing = ingredients[param.id] ?: return failure(NotFoundError("RecipeIngredient", param.id.toString()))
        val updated = existing.copy(
            ingredientId = param.ingredientId ?: existing.ingredientId,
            quantity = param.quantity ?: existing.quantity,
            unit = param.unit ?: existing.unit,
            status = param.status ?: existing.status,
            matchedIngredientId = when {
                param.clearMatchedIngredient -> null
                param.matchedIngredientId != null -> param.matchedIngredientId
                else -> existing.matchedIngredientId
            },
            suggestedIngredientName = param.suggestedIngredientName ?: existing.suggestedIngredientName,
            reviewFlags = param.reviewFlags ?: existing.reviewFlags,
            updatedAt = Instant.now()
        )
        ingredients[param.id] = updated
        return success(updated)
    }

    override fun removeIngredient(param: RemoveRecipeIngredientParam): Result<Unit, AppError> {
        if (!ingredients.containsKey(param.id)) return failure(NotFoundError("RecipeIngredient", param.id.toString()))
        ingredients.remove(param.id)
        return success(Unit)
    }

    fun reset() {
        recipes.clear()
        ingredients.clear()
    }

    fun seed(vararg entities: Recipe) = entities.forEach { recipes[it.id] = it }

    fun seedIngredients(vararg entities: RecipeIngredient) = entities.forEach { ingredients[it.id] = it }
}
