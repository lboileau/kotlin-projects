package com.acmo.clients.worldclient.fake

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.ConflictError
import com.acmo.clients.common.error.NotFoundError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.CreateWorldParam
import com.acmo.clients.worldclient.api.DeleteWorldParam
import com.acmo.clients.worldclient.api.GetByIdParam
import com.acmo.clients.worldclient.api.GetListParam
import com.acmo.clients.worldclient.api.UpdateWorldParam
import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.clients.worldclient.internal.validations.ValidateCreateWorld
import com.acmo.clients.worldclient.internal.validations.ValidateDeleteWorld
import com.acmo.clients.worldclient.internal.validations.ValidateGetWorldById
import com.acmo.clients.worldclient.internal.validations.ValidateGetWorldList
import com.acmo.clients.worldclient.internal.validations.ValidateUpdateWorld
import com.acmo.clients.worldclient.model.World
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeWorldClient : WorldClient {
    private val store = ConcurrentHashMap<UUID, World>()

    private companion object {
        const val MAX_LIMIT = 100
    }

    private val validateGetById = ValidateGetWorldById()
    private val validateGetList = ValidateGetWorldList()
    private val validateCreate = ValidateCreateWorld()
    private val validateUpdate = ValidateUpdateWorld()
    private val validateDelete = ValidateDeleteWorld()

    override fun getById(param: GetByIdParam): Result<World, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val entity = store[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("World", param.id.toString()))
    }

    override fun getList(param: GetListParam): Result<List<World>, AppError> {
        val validation = validateGetList.execute(param)
        if (validation is Result.Failure) return validation

        val effectiveLimit = minOf(param.limit ?: MAX_LIMIT, MAX_LIMIT)
        var entities = store.values.sortedBy { it.name }
        if (param.offset != null) entities = entities.drop(param.offset)
        entities = entities.take(effectiveLimit)
        return success(entities)
    }

    override fun create(param: CreateWorldParam): Result<World, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (store.values.any { it.name == param.name }) {
            return failure(ConflictError("World", "name '${param.name}' already exists"))
        }
        val entity = World(
            id = UUID.randomUUID(),
            name = param.name,
            greeting = param.greeting,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun update(param: UpdateWorldParam): Result<World, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("World", param.id.toString()))
        val updatedName = param.name ?: existing.name
        if (param.name != null && store.values.any { it.name == updatedName && it.id != param.id }) {
            return failure(ConflictError("World", "name '$updatedName' already exists"))
        }
        val updated = existing.copy(
            name = updatedName,
            greeting = param.greeting ?: existing.greeting,
            updatedAt = Instant.now()
        )
        store[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteWorldParam): Result<Unit, AppError> {
        val validation = validateDelete.execute(param)
        if (validation is Result.Failure) return validation

        return if (store.remove(param.id) != null) success(Unit) else failure(NotFoundError("World", param.id.toString()))
    }

    fun reset() = store.clear()

    fun seed(vararg entities: World) {
        entities.forEach { store[it.id] = it }
    }
}
