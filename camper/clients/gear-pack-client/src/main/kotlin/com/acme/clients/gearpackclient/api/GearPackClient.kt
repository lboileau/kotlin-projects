package com.acme.clients.gearpackclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearpackclient.model.GearPack

/**
 * Client interface for GearPack entity read operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface GearPackClient {

    /** Retrieve all available gear packs (without items). */
    fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError>

    /** Retrieve a gear pack by ID with its items. */
    fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError>
}
