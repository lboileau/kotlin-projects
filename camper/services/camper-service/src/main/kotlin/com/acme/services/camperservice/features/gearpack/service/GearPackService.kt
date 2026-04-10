package com.acme.services.camperservice.features.gearpack.service

import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.actions.AddGearPackItemAction
import com.acme.services.camperservice.features.gearpack.actions.ApplyGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.CreateGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.DeleteGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.GetGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.ListGearPacksAction
import com.acme.services.camperservice.features.gearpack.actions.RemoveGearPackItemAction
import com.acme.services.camperservice.features.gearpack.actions.SearchGearPackItemsAction
import com.acme.services.camperservice.features.gearpack.actions.UpdateGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.UpdateGearPackItemAction
import com.acme.services.camperservice.features.gearpack.params.AddGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.CreateGearPackParam
import com.acme.services.camperservice.features.gearpack.params.DeleteGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import com.acme.services.camperservice.features.gearpack.params.RemoveGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.SearchGearPackItemsParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackParam

class GearPackService(
    gearPackClient: GearPackClient,
    itemClient: ItemClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val listGearPacks = ListGearPacksAction(gearPackClient)
    private val getGearPack = GetGearPackAction(gearPackClient)
    private val applyGearPack = ApplyGearPackAction(gearPackClient, itemClient, planRoleAuthorizer)
    private val createGearPack = CreateGearPackAction(gearPackClient)
    private val updateGearPack = UpdateGearPackAction(gearPackClient)
    private val deleteGearPack = DeleteGearPackAction(gearPackClient)
    private val addGearPackItem = AddGearPackItemAction(gearPackClient)
    private val updateGearPackItem = UpdateGearPackItemAction(gearPackClient)
    private val removeGearPackItem = RemoveGearPackItemAction(gearPackClient)
    private val searchGearPackItems = SearchGearPackItemsAction(gearPackClient)

    fun list(param: ListGearPacksParam) = listGearPacks.execute(param)
    fun getById(param: GetGearPackParam) = getGearPack.execute(param)
    fun apply(param: ApplyGearPackParam) = applyGearPack.execute(param)
    fun create(param: CreateGearPackParam) = createGearPack.execute(param)
    fun update(param: UpdateGearPackParam) = updateGearPack.execute(param)
    fun delete(param: DeleteGearPackParam) = deleteGearPack.execute(param)
    fun addItem(param: AddGearPackItemParam) = addGearPackItem.execute(param)
    fun updateItem(param: UpdateGearPackItemParam) = updateGearPackItem.execute(param)
    fun removeItem(param: RemoveGearPackItemParam) = removeGearPackItem.execute(param)
    fun searchItems(param: SearchGearPackItemsParam) = searchGearPackItems.execute(param)
}
