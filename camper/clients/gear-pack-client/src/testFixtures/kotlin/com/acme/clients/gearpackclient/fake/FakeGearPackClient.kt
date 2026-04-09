package com.acme.clients.gearpackclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
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
import com.acme.clients.gearpackclient.internal.validations.ValidateGetAllGearPacks
import com.acme.clients.gearpackclient.internal.validations.ValidateGetGearPackById
import com.acme.clients.gearpackclient.model.GearPack
import com.acme.clients.gearpackclient.model.GearPackItem
import com.acme.clients.gearpackclient.model.GearPackItemSearchResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeGearPackClient : GearPackClient {
    private val store = ConcurrentHashMap<UUID, GearPack>()

    private val validateGetAll = ValidateGetAllGearPacks()
    private val validateGetById = ValidateGetGearPackById()

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

    override fun create(param: CreateGearPackParam): Result<GearPack, AppError> = TODO()
    override fun update(param: UpdateGearPackParam): Result<GearPack, AppError> = TODO()
    override fun delete(param: DeleteGearPackParam): Result<Unit, AppError> = TODO()
    override fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError> = TODO()
    override fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError> = TODO()
    override fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError> = TODO()
    override fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError> = TODO()

    fun reset() = store.clear()

    fun seedGearPack(vararg packs: GearPack) {
        packs.forEach { store[it.id] = it }
    }
}
