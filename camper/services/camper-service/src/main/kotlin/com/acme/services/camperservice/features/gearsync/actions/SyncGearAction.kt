package com.acme.services.camperservice.features.gearsync.actions

import com.acme.clients.common.Result
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.services.camperservice.features.gearsync.dto.GearSyncItemResponse
import com.acme.services.camperservice.features.gearsync.dto.GearSyncResponse
import com.acme.services.camperservice.features.gearsync.error.GearSyncError
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam
import com.acme.clients.gearsyncclient.api.SyncGearParam as ClientSyncGearParam

internal class SyncGearAction(
    private val gearSyncClient: GearSyncClient,
) {
    fun execute(param: SyncGearParam): Result<GearSyncResponse, GearSyncError> {
        return when (val result = gearSyncClient.sync(ClientSyncGearParam(param.planId))) {
            is Result.Success -> Result.Success(
                GearSyncResponse(
                    items = result.value.map { GearSyncItemResponse(it.name, it.category, it.quantity) }
                )
            )
            is Result.Failure -> Result.Failure(GearSyncError.PlanNotFound(param.planId.toString()))
        }
    }
}
