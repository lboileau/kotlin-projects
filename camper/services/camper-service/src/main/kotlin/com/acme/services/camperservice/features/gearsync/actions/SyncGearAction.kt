package com.acme.services.camperservice.features.gearsync.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.gearsync.dto.GearSyncResponse
import com.acme.services.camperservice.features.gearsync.error.GearSyncError
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam

internal class SyncGearAction(
    private val assignmentClient: AssignmentClient,
    private val itemClient: ItemClient,
    private val planClient: PlanClient,
) {
    fun execute(param: SyncGearParam): Result<GearSyncResponse, GearSyncError> {
        TODO("Not yet implemented")
    }
}
