package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.model.GearPack
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateGetGearPack
import org.slf4j.LoggerFactory

internal class GetGearPackAction(private val gearPackClient: GearPackClient) {
    private val logger = LoggerFactory.getLogger(GetGearPackAction::class.java)
    private val validate = ValidateGetGearPack()

    fun execute(param: GetGearPackParam): Result<GearPack, GearPackError> = TODO()
}
