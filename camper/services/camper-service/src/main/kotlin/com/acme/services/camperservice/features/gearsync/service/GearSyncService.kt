package com.acme.services.camperservice.features.gearsync.service

import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.services.camperservice.features.gearsync.actions.SyncGearAction
import com.acme.services.camperservice.features.gearsync.params.SyncGearParam

class GearSyncService(
    gearSyncClient: GearSyncClient,
) {
    private val syncGear = SyncGearAction(gearSyncClient)

    fun sync(param: SyncGearParam) = syncGear.execute(param)
}
