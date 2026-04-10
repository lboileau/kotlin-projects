package com.acme.clients.gearpackclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.AddGearPackItemParam
import com.acme.clients.gearpackclient.api.CreateGearPackParam
import com.acme.clients.gearpackclient.api.DeleteGearPackParam
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.RemoveGearPackItemParam
import com.acme.clients.gearpackclient.api.SearchGearPackItemsParam
import com.acme.clients.gearpackclient.api.UpdateGearPackItemParam
import com.acme.clients.gearpackclient.api.UpdateGearPackParam
import com.acme.clients.gearpackclient.internal.validations.ValidateAddGearPackItem
import com.acme.clients.gearpackclient.internal.validations.ValidateCreateGearPack
import com.acme.clients.gearpackclient.internal.validations.ValidateGetAllGearPacks
import com.acme.clients.gearpackclient.internal.validations.ValidateGetGearPackById
import com.acme.clients.gearpackclient.internal.validations.ValidateSearchGearPackItems
import com.acme.clients.gearpackclient.internal.validations.ValidateUpdateGearPack
import com.acme.clients.gearpackclient.internal.validations.ValidateUpdateGearPackItem
import com.acme.clients.gearpackclient.model.GearPack
import com.acme.clients.gearpackclient.model.GearPackItem
import com.acme.clients.gearpackclient.model.GearPackItemSearchResult
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeGearPackClient : GearPackClient {
    private val store = ConcurrentHashMap<UUID, GearPack>()

    private val validateGetAll = ValidateGetAllGearPacks()
    private val validateGetById = ValidateGetGearPackById()
    private val validateCreate = ValidateCreateGearPack()
    private val validateUpdate = ValidateUpdateGearPack()
    private val validateAddItem = ValidateAddGearPackItem()
    private val validateUpdateItem = ValidateUpdateGearPackItem()
    private val validateSearch = ValidateSearchGearPackItems()

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> {
        val validation = validateGetAll.execute(param)
        if (validation is Result.Failure) return validation

        return success(store.values.sortedBy { it.name }.toList())
    }

    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> {
        val validation = validateGetById.execute(param)
        if (validation is Result.Failure) return validation

        val pack = store[param.id] ?: return failure(NotFoundError("GearPack", param.id.toString()))
        return success(pack)
    }

    override fun create(param: CreateGearPackParam): Result<GearPack, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (store.values.any { it.name.equals(param.name, ignoreCase = true) }) {
            return failure(ConflictError("GearPack", "name '${param.name}' already exists"))
        }
        val entity = GearPack(
            id = UUID.randomUUID(),
            name = param.name,
            description = param.description,
            items = emptyList(),
            createdBy = param.createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        store[entity.id] = entity
        return success(entity)
    }

    override fun update(param: UpdateGearPackParam): Result<GearPack, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = store[param.id] ?: return failure(NotFoundError("GearPack", param.id.toString()))
        val newName = param.name ?: existing.name
        if (param.name != null && store.values.any { it.name.equals(newName, ignoreCase = true) && it.id != param.id }) {
            return failure(ConflictError("GearPack", "name '$newName' already exists"))
        }
        val updated = existing.copy(
            name = newName,
            description = param.description ?: existing.description,
            updatedAt = Instant.now(),
        )
        store[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteGearPackParam): Result<Unit, AppError> {
        if (!store.containsKey(param.id)) return failure(NotFoundError("GearPack", param.id.toString()))
        store.remove(param.id)
        return success(Unit)
    }

    override fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError> {
        val validation = validateAddItem.execute(param)
        if (validation is Result.Failure) return validation

        val pack = store[param.gearPackId] ?: return failure(NotFoundError("GearPack", param.gearPackId.toString()))
        if (pack.items.any { it.name.equals(param.name, ignoreCase = true) }) {
            return failure(ConflictError("GearPackItem", "name '${param.name}' already exists in this gear pack"))
        }
        val sortOrder = (pack.items.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val item = GearPackItem(
            id = UUID.randomUUID(),
            gearPackId = param.gearPackId,
            name = param.name,
            category = param.category,
            defaultQuantity = param.defaultQuantity,
            scalable = param.scalable,
            sortOrder = sortOrder,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        store[param.gearPackId] = pack.copy(items = pack.items + item)
        return success(item)
    }

    override fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError> {
        val validation = validateUpdateItem.execute(param)
        if (validation is Result.Failure) return validation

        val pack = store[param.gearPackId] ?: return failure(NotFoundError("GearPack", param.gearPackId.toString()))
        val existing = pack.items.find { it.id == param.id }
            ?: return failure(NotFoundError("GearPackItem", param.id.toString()))

        val newName = param.name ?: existing.name
        if (param.name != null && pack.items.any { it.name.equals(newName, ignoreCase = true) && it.id != param.id }) {
            return failure(ConflictError("GearPackItem", "name '$newName' already exists in this gear pack"))
        }

        val updated = existing.copy(
            name = newName,
            category = param.category ?: existing.category,
            defaultQuantity = param.defaultQuantity ?: existing.defaultQuantity,
            scalable = param.scalable ?: existing.scalable,
            updatedAt = Instant.now(),
        )
        store[param.gearPackId] = pack.copy(items = pack.items.map { if (it.id == param.id) updated else it })
        return success(updated)
    }

    override fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError> {
        val pack = store[param.gearPackId] ?: return failure(NotFoundError("GearPack", param.gearPackId.toString()))
        if (pack.items.none { it.id == param.id }) return failure(NotFoundError("GearPackItem", param.id.toString()))
        store[param.gearPackId] = pack.copy(items = pack.items.filter { it.id != param.id })
        return success(Unit)
    }

    override fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError> {
        val validation = validateSearch.execute(param)
        if (validation is Result.Failure) return validation

        val query = param.query.lowercase()
        val results = store.values.flatMap { pack ->
            pack.items
                .filter { item ->
                    val name = item.name.lowercase()
                    name.contains(query) || levenshtein(name, query) <= 3
                }
                .map { item ->
                    GearPackItemSearchResult(
                        id = item.id,
                        gearPackId = pack.id,
                        gearPackName = pack.name,
                        name = item.name,
                        category = item.category,
                        defaultQuantity = item.defaultQuantity,
                        scalable = item.scalable,
                    )
                }
        }.sortedWith(
            compareBy(
                { item ->
                    val name = item.name.lowercase()
                    when {
                        name == query -> 0
                        name.startsWith(query) -> 1
                        name.contains(query) -> 2
                        else -> 3
                    }
                },
                { item -> levenshtein(item.name.lowercase(), query) },
            )
        ).take(20)
        return success(results)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }

    fun reset() = store.clear()

    fun seedGearPack(vararg packs: GearPack) {
        packs.forEach { store[it.id] = it }
    }
}
