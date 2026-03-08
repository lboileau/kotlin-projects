package com.acme.services.camperservice.features.gearsync.service

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.gearsync.actions.SyncGearAction
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam

class GearSyncService(
    assignmentClient: AssignmentClient,
    itemClient: ItemClient,
    planClient: PlanClient,
) {
    private val syncGear = SyncGearAction(assignmentClient, itemClient, planClient)

    fun sync(param: SyncGearParam) = syncGear.execute(param)
}
