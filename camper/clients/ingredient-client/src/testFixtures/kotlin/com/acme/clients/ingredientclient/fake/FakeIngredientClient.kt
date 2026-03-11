package com.acme.clients.ingredientclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.*
import com.acme.clients.ingredientclient.internal.validations.ValidateCreateIngredient
import com.acme.clients.ingredientclient.internal.validations.ValidateUpdateIngredient
import com.acme.clients.ingredientclient.model.Ingredient
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeIngredientClient : IngredientClient {
    private val store = ConcurrentHashMap<UUID, Ingredient>()
    private val validateCreate = ValidateCreateIngredient()
    private val validateUpdate = ValidateUpdateIngredient()

    override fun create(param: CreateIngredientParam): Result<Ingredient, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (store.values.any { it.name.equals(param.name, ignoreCase = true) }) {
            return failure(ConflictError("Ingredient", "name '${param.name}' already exists"))
        }
        val entity = Ingredient(
            id = UUID.randomUUID(),
            name = param.name,
            category = param.category,
            defaultUnit = param.defaultUnit,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<Ingredient, AppError> {
        val entity = store[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("Ingredient", param.id.toString()))
    }

    override fun getAll(): Result<List<Ingredient>, AppError> {
        return success(store.values.sortedBy { it.name })
    }

    override fun update(param: UpdateIngredientParam): Result<Ingredient, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("Ingredient", param.id.toString()))
        val newName = param.name ?: existing.name
        if (param.name != null && store.values.any { it.name.equals(newName, ignoreCase = true) && it.id != param.id }) {
            return failure(ConflictError("Ingredient", "name '$newName' already exists"))
        }
        val updated = existing.copy(
            name = newName,
            category = param.category ?: existing.category,
            defaultUnit = param.defaultUnit ?: existing.defaultUnit,
            updatedAt = Instant.now()
        )
        store[param.id] = updated
        return success(updated)
    }

    override fun findByName(param: FindByNameParam): Result<Ingredient?, AppError> {
        return success(store.values.find { it.name.equals(param.name, ignoreCase = true) })
    }

    override fun findByNames(param: FindByNamesParam): Result<List<Ingredient>, AppError> {
        val lowerNames = param.names.map { it.lowercase() }.toSet()
        return success(store.values.filter { it.name.lowercase() in lowerNames }.sortedBy { it.name })
    }

    override fun createBatch(param: CreateBatchParam): Result<List<Ingredient>, AppError> {
        val results = mutableListOf<Ingredient>()
        for (ing in param.ingredients) {
            val result = create(ing)
            if (result is Result.Failure) return result
            results.add((result as Result.Success).value)
        }
        return success(results)
    }

    override fun delete(param: DeleteIngredientParam): Result<Unit, AppError> {
        if (!store.containsKey(param.id)) return failure(NotFoundError("Ingredient", param.id.toString()))
        store.remove(param.id)
        return success(Unit)
    }

    fun reset() = store.clear()

    fun seed(vararg entities: Ingredient) = entities.forEach { store[it.id] = it }
}
