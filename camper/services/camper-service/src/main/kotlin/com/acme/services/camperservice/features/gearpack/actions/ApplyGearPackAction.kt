package com.acme.services.camperservice.features.gearpack.actions

import com.acme.clients.common.Result
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.error.GearPackError
import com.acme.services.camperservice.features.gearpack.model.ApplyGearPackResult
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.validations.ValidateApplyGearPack
import org.slf4j.LoggerFactory

internal class ApplyGearPackAction(
    private val gearPackClient: GearPackClient,
    private val itemClient: ItemClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(ApplyGearPackAction::class.java)
    private val validate = ValidateApplyGearPack()

    fun execute(param: ApplyGearPackParam): Result<ApplyGearPackResult, GearPackError> = TODO()
}
