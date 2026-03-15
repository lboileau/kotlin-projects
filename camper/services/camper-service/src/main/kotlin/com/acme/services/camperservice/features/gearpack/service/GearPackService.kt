package com.acme.services.camperservice.features.gearpack.service

import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.actions.ApplyGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.GetGearPackAction
import com.acme.services.camperservice.features.gearpack.actions.ListGearPacksAction
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam

class GearPackService(
    gearPackClient: GearPackClient,
    itemClient: ItemClient,
    planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val listGearPacks = ListGearPacksAction(gearPackClient)
    private val getGearPack = GetGearPackAction(gearPackClient)
    private val applyGearPack = ApplyGearPackAction(gearPackClient, itemClient, planRoleAuthorizer)

    fun list(param: ListGearPacksParam) = listGearPacks.execute(param)
    fun getById(param: GetGearPackParam) = getGearPack.execute(param)
    fun apply(param: ApplyGearPackParam) = applyGearPack.execute(param)
}
