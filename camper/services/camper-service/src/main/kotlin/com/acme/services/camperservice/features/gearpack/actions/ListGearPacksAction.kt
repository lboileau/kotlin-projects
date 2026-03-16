package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateListGearPacks
import org.slf4j.LoggerFactory

internal class ListGearPacksAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(ListGearPacksAction::class.java)
    private val validate = ValidateListGearPacks()

    fun execute(param: ListGearPacksParam): Result<List<GearPack>, GearPackError> = TODO()
}
