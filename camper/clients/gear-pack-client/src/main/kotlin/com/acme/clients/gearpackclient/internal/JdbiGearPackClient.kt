package com.acme.clients.gearpackclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.api.GetAllGearPacksParam
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.internal.operations.GetAllGearPacks
import com.acme.clients.gearpackclient.internal.operations.GetGearPackById
import com.acme.clients.gearpackclient.model.GearPack
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiGearPackClient(jdbi: Jdbi) : GearPackClient {

    private val getAllGearPacks = GetAllGearPacks(jdbi)
    private val getGearPackById = GetGearPackById(jdbi)

    override fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError> = getAllGearPacks.execute(param)
    override fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError> = getGearPackById.execute(param)
}
