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
import com.acme.clients.gearpackclient.internal.operations.AddGearPackItem
import com.acme.clients.gearpackclient.internal.operations.CreateGearPack
import com.acme.clients.gearpackclient.internal.operations.DeleteGearPack
import com.acme.clients.gearpackclient.internal.operations.GetAllGearPacks
import com.acme.clients.gearpackclient.internal.operations.GetGearPackById
import com.acme.clients.gearpackclient.internal.operations.RemoveGearPackItem
import com.acme.clients.gearpackclient.internal.operations.SearchGearPackItems
import com.acme.clients.gearpackclient.internal.operations.UpdateGearPack
import com.acme.clients.gearpackclient.internal.operations.UpdateGearPackItem
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
    private val createGearPack = CreateGearPack(jdbi)
    private val updateGearPack = UpdateGearPack(jdbi, getGearPackById)
    private val deleteGearPack = DeleteGearPack(jdbi)
    private val addGearPackItem = AddGearPackItem(jdbi)
    private val updateGearPackItem = UpdateGearPackItem(jdbi)
    private val removeGearPackItem = RemoveGearPackItem(jdbi)
    private val searchGearPackItems = SearchGearPackItems(jdbi)

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> = getAllGearPacks.execute(param)
    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> = getGearPackById.execute(param)
    override fun create(param: CreateGearPackParam): Result<GearPack, AppError> = createGearPack.execute(param)
    override fun update(param: UpdateGearPackParam): Result<GearPack, AppError> = updateGearPack.execute(param)
    override fun delete(param: DeleteGearPackParam): Result<Unit, AppError> = deleteGearPack.execute(param)
    override fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError> = addGearPackItem.execute(param)
    override fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError> = updateGearPackItem.execute(param)
    override fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError> = removeGearPackItem.execute(param)
    override fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError> = searchGearPackItems.execute(param)
}
