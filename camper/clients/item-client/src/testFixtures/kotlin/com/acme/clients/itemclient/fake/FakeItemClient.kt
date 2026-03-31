package com.acme.clients.itemclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.*
import com.acme.clients.itemclient.internal.validations.*
import com.acme.clients.itemclient.model.Item
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeItemClient : ItemClient {
    private val store = ConcurrentHashMap<UUID, Item>()

    private val validateCreate = ValidateCreateItem()
    private val validateGetById = ValidateGetItemById()
    private val validateGetByPlanId = ValidateGetItemsByPlanId()
    private val validateGetByUserId = ValidateGetItemsByUserId()
    private val validateGetByPlanIdAndUserId = ValidateGetItemsByPlanIdAndUserId()
    private val validateUpdate = ValidateUpdateItem()
    private val validateDelete = ValidateDeleteItem()

    override fun create(param: CreateItemParam): Result<Item, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        val entity = Item(
            id = UUID.randomUUID(),
            planId = param.planId,
            userId = param.userId,
            name = param.name,
            category = param.category,
            quantity = param.quantity,
            packed = param.packed,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<Item, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("Item", param.id.toString()))
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Item>, AppError> {
        val validation = validateGetByPlanId.execute(param)
        if (validation is Result.Failure) return validation

        val entities = store.values.filter { it.planId == param.planId && it.userId == null }.sortedBy { it.createdAt }
        return success(entities)
    }

    override fun getByUserId(param: GetByUserIdParam): Result<List<Item>, AppError> {
        val validation = validateGetByUserId.execute(param)
        if (validation is Result.Failure) return validation

        val entities = store.values.filter { it.userId == param.userId }.sortedBy { it.createdAt }
        return success(entities)
    }

    override fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<List<Item>, AppError> {
        val validation = validateGetByPlanIdAndUserId.execute(param)
        if (validation is Result.Failure) return validation

        val entities = store.values.filter { it.planId == param.planId && it.userId == param.userId }.sortedBy { it.createdAt }
        return success(entities)
    }

    override fun update(param: UpdateItemParam): Result<Item, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("Item", param.id.toString()))
        val updated = existing.copy(
            name = param.name,
            category = param.category,
            quantity = param.quantity,
            packed = param.packed,
            updatedAt = Instant.now()
        )
        store[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteItemParam): Result<Unit, AppError> {
        val validation = validateDelete.execute(param)
        if (validation is Result.Failure) return validation

        return if (store.remove(param.id) != null) success(Unit) else failure(NotFoundError("Item", param.id.toString()))
    }

    fun reset() = store.clear()

    fun seedItem(vararg entities: Item) {
        entities.forEach { store[it.id] = it }
    }
}
