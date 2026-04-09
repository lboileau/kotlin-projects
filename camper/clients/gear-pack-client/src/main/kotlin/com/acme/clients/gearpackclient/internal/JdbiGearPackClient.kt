package com.acme.clients.gearpackclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
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
import com.acme.clients.gearpackclient.internal.operations.GetAllGearPacks
import com.acme.clients.gearpackclient.internal.operations.GetGearPackById
import com.acme.clients.gearpackclient.model.GearPack
import com.acme.clients.gearpackclient.model.GearPackItem
import com.acme.clients.gearpackclient.model.GearPackItemSearchResult
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiGearPackClient(jdbi: Jdbi) : GearPackClient {

    private val getAllGearPacks = GetAllGearPacks(jdbi)
    private val getGearPackById = GetGearPackById(jdbi)

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> = getAllGearPacks.execute(param)
    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> = getGearPackById.execute(param)

    override fun create(param: CreateGearPackParam): Result<GearPack, AppError> = TODO()
    override fun update(param: UpdateGearPackParam): Result<GearPack, AppError> = TODO()
    override fun delete(param: DeleteGearPackParam): Result<Unit, AppError> = TODO()
    override fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError> = TODO()
    override fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError> = TODO()
    override fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError> = TODO()
    override fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError> = TODO()
}
