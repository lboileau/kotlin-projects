package com.acme.clients.gearsyncclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearsyncclient.model.GearSyncItem

interface GearSyncClient {
    fun sync(param: SyncGearParam): Result<List<GearSyncItem>, AppError>
}
