package com.acme.clients.recipeclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.*
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeFavorite
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipePhoto
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredient
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeIngredients
import com.acme.clients.recipeclient.internal.validations.ValidateCreateRecipe
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeFavoriteSummaries
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeFavorites
import com.acme.clients.recipeclient.internal.validations.ValidateRemoveRecipeFavorite
import com.acme.clients.recipeclient.internal.validations.ValidateReplaceRecipeSteps
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipe
import com.acme.clients.recipeclient.internal.validations.ValidateUpdateRecipeIngredient
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeFavorite
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.clients.recipeclient.model.RecipeStep
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeRecipeClient : RecipeClient {
    private val recipes = ConcurrentHashMap<UUID, Recipe>()
    private val ingredients = ConcurrentHashMap<UUID, RecipeIngredient>()

    /**
     * Keyed by `(recipeId, userId)` — the in-memory form of the
     * `uq_recipe_favorites_recipe_user` unique constraint, which is what makes both
     * favourite mutations idempotent for free.
     */
    private val favorites = ConcurrentHashMap<Pair<UUID, UUID>, RecipeFavorite>()
    private val steps = ConcurrentHashMap<UUID, RecipeStep>()
    private val photos = ConcurrentHashMap<UUID, RecipePhoto>()

    private val validateCreate = ValidateCreateRecipe()
    private val validateUpdate = ValidateUpdateRecipe()
    private val validateAddIngredient = ValidateAddRecipeIngredient()
    private val validateAddIngredients = ValidateAddRecipeIngredients()
    private val validateUpdateIngredient = ValidateUpdateRecipeIngredient()
    private val validateAddFavorite = ValidateAddRecipeFavorite()
    private val validateRemoveFavorite = ValidateRemoveRecipeFavorite()
    private val validateGetFavorites = ValidateGetRecipeFavorites()
    private val validateGetFavoriteSummaries = ValidateGetRecipeFavoriteSummaries()
    private val validateReplaceSteps = ValidateReplaceRecipeSteps()
    private val validateAddPhoto = ValidateAddRecipePhoto()

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
            meal = param.meal,
            theme = param.theme,
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
            description = when {
                param.clearDescription -> null
                param.description != null -> param.description
                else -> existing.description
            },
            baseServings = param.baseServings ?: existing.baseServings,
            status = param.status ?: existing.status,
            duplicateOfId = when {
                param.clearDuplicateOf -> null
                param.duplicateOfId != null -> param.duplicateOfId
                else -> existing.duplicateOfId
            },
            meal = when {
                param.clearMeal -> null
                param.meal != null -> param.meal
                else -> existing.meal
            },
            theme = when {
                param.clearTheme -> null
                param.theme != null -> param.theme
                else -> existing.theme
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
        // In-memory stand-in for fk_recipe_favorites_recipe ON DELETE CASCADE.
        favorites.keys.removeIf { it.first == param.id }
        steps.values.removeIf { it.recipeId == param.id }
        photos.values.removeIf { it.recipeId == param.id }
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
            suggestedCategory = param.suggestedCategory,
            suggestedUnit = param.suggestedUnit,
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

    override fun findIngredientsByIngredientId(param: FindRecipeIngredientsByIngredientIdParam): Result<List<RecipeIngredient>, AppError> {
        return success(ingredients.values.filter { it.ingredientId == param.ingredientId }.sortedBy { it.createdAt })
    }

    override fun addFavorite(param: AddRecipeFavoriteParam): Result<Unit, AppError> {
        val validation = validateAddFavorite.execute(param)
        if (validation is Result.Failure) return validation

        // putIfAbsent mirrors ON CONFLICT DO NOTHING: re-favouriting leaves the existing
        // row — and therefore its createdAt — untouched, keeping list order stable.
        favorites.putIfAbsent(
            param.recipeId to param.userId,
            RecipeFavorite(
                id = UUID.randomUUID(),
                recipeId = param.recipeId,
                userId = param.userId,
                createdAt = Instant.now()
            )
        )
        return success(Unit)
    }

    override fun removeFavorite(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> {
        val validation = validateRemoveFavorite.execute(param)
        if (validation is Result.Failure) return validation

        // Idempotent: success whether or not a row was there, matching RemoveRecipeFavorite.
        favorites.remove(param.recipeId to param.userId)
        return success(Unit)
    }

    override fun getFavorites(param: GetRecipeFavoritesParam): Result<List<RecipeFavorite>, AppError> {
        val validation = validateGetFavorites.execute(param)
        if (validation is Result.Failure) return validation

        return success(
            favorites.values
                .filter { it.recipeId == param.recipeId }
                .sortedWith(compareBy({ it.createdAt }, { it.id }))
        )
    }

    override fun getFavoriteSummaries(param: GetRecipeFavoriteSummariesParam): Result<List<RecipeFavoriteSummary>, AppError> {
        val validation = validateGetFavoriteSummaries.execute(param)
        if (validation is Result.Failure) return validation

        if (param.recipeIds.isEmpty()) return success(emptyList())

        val wanted = param.recipeIds.toSet()
        // Recipes with no favourites are ABSENT from the result, exactly like the SQL's
        // GROUP BY — never zero-filled. Callers must default a missing id themselves.
        val summaries = favorites.values
            .filter { it.recipeId in wanted }
            .groupBy { it.recipeId }
            .map { (recipeId, rows) ->
                RecipeFavoriteSummary(
                    recipeId = recipeId,
                    favoriteCount = rows.size,
                    favoritedByMe = rows.any { it.userId == param.userId }
                )
            }
        return success(summaries)
    }

    override fun getSteps(param: GetRecipeStepsParam): Result<List<RecipeStep>, AppError> =
        success(steps.values.filter { it.recipeId == param.recipeId }.sortedBy { it.position })

    override fun replaceSteps(param: ReplaceRecipeStepsParam): Result<List<RecipeStep>, AppError> {
        val validation = validateReplaceSteps.execute(param)
        if (validation is Result.Failure) return validation

        steps.values.removeIf { it.recipeId == param.recipeId }
        val now = Instant.now()
        val created = param.texts.mapIndexed { position, text ->
            RecipeStep(id = UUID.randomUUID(), recipeId = param.recipeId, position = position, text = text.trim(), createdAt = now)
        }
        created.forEach { steps[it.id] = it }
        return success(created)
    }

    override fun getPhotos(param: GetRecipePhotosParam): Result<List<RecipePhoto>, AppError> =
        success(photos.values.filter { it.recipeId == param.recipeId }.sortedBy { it.position })

    override fun getPhotoById(param: GetRecipePhotoByIdParam): Result<RecipePhoto, AppError> =
        photos[param.id]?.let { success(it) } ?: failure(NotFoundError("RecipePhoto", param.id.toString()))

    override fun addPhoto(param: AddRecipePhotoParam): Result<RecipePhoto, AppError> {
        val validation = validateAddPhoto.execute(param)
        if (validation is Result.Failure) return validation

        if (photos.values.any { it.storageKey == param.storageKey }) {
            return failure(ConflictError("RecipePhoto", "storage key already used: ${param.storageKey}"))
        }
        val position = (photos.values.filter { it.recipeId == param.recipeId }.maxOfOrNull { it.position } ?: -1) + 1
        val photo = RecipePhoto(
            id = param.id, recipeId = param.recipeId, position = position, storageKey = param.storageKey,
            mediaType = param.mediaType, byteSize = param.byteSize, width = param.width, height = param.height,
            source = param.source, role = param.role, createdBy = param.createdBy, createdAt = Instant.now()
        )
        photos[photo.id] = photo
        return success(photo)
    }

    override fun removePhoto(param: RemoveRecipePhotoParam): Result<Unit, AppError> =
        if (photos.remove(param.id) != null) success(Unit) else failure(NotFoundError("RecipePhoto", param.id.toString()))

    fun reset() {
        recipes.clear()
        ingredients.clear()
        favorites.clear()
        steps.clear()
        photos.clear()
    }

    fun seed(vararg entities: Recipe) = entities.forEach { recipes[it.id] = it }

    fun seedIngredients(vararg entities: RecipeIngredient) = entities.forEach { ingredients[it.id] = it }

    fun seedFavorites(vararg entities: RecipeFavorite) =
        entities.forEach { favorites[it.recipeId to it.userId] = it }
}
