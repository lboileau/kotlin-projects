package com.acme.clients.gearsyncclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.clients.gearsyncclient.api.SyncGearParam
import com.acme.clients.gearsyncclient.model.GearSyncItem

class FakeGearSyncClient : GearSyncClient {
    private val syncResults = mutableMapOf<String, List<GearSyncItem>>()
    private val syncCalls = mutableListOf<SyncGearParam>()

    override fun sync(param: SyncGearParam): Result<List<GearSyncItem>, AppError> {
        syncCalls.add(param)
        val items = syncResults[param.planId.toString()] ?: emptyList()
        return Result.Success(items)
    }

    fun reset() {
        syncResults.clear()
        syncCalls.clear()
    }

    fun setSyncResult(planId: String, items: List<GearSyncItem>) {
        syncResults[planId] = items
    }

    fun getSyncCalls(): List<SyncGearParam> = syncCalls.toList()
}
