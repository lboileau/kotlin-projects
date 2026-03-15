package com.acme.clients.gearpackclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.internal.validations.ValidateGetAllGearPacks
import com.acme.clients.gearpackclient.internal.validations.ValidateGetGearPackById
import com.acme.clients.gearpackclient.model.GearPack
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

    fun reset() = store.clear()

    fun seedGearPack(vararg packs: GearPack) {
        packs.forEach { store[it.id] = it }
    }
}
