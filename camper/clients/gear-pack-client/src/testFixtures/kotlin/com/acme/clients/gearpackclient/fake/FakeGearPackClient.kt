package com.acme.clients.gearpackclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.model.GearPack

class FakeGearPackClient : GearPackClient {

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> {
        throw NotImplementedError()
    }

    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> {
        throw NotImplementedError()
    }
}
