package com.acme.clients.gearpackclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.gearpackclient.model.GearPack
import com.acme.clients.gearpackclient.model.GearPackItem
import com.acme.clients.gearpackclient.model.GearPackItemSearchResult

/**
 * Client interface for GearPack entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface GearPackClient {

    /** Retrieve all available gear packs (without items). */
    fun getAll(param: GetAllGearPacksParam): Result<List<GearPack>, AppError>

    /** Retrieve a gear pack by ID with its items. */
    fun getById(param: GetGearPackByIdParam): Result<GearPack, AppError>

    /** Create a new gear pack. */
    fun create(param: CreateGearPackParam): Result<GearPack, AppError>

    /** Update an existing gear pack. Null fields are left unchanged. */
    fun update(param: UpdateGearPackParam): Result<GearPack, AppError>

    /** Delete a gear pack by ID. Returns NotFoundError if not found. */
    fun delete(param: DeleteGearPackParam): Result<Unit, AppError>

    /** Add an item to a gear pack. */
    fun addItem(param: AddGearPackItemParam): Result<GearPackItem, AppError>

    /** Update an item within a gear pack. Null fields are left unchanged. */
    fun updateItem(param: UpdateGearPackItemParam): Result<GearPackItem, AppError>

    /** Remove an item from a gear pack. */
    fun removeItem(param: RemoveGearPackItemParam): Result<Unit, AppError>

    /** Search gear pack items by name (case-insensitive substring match). */
    fun searchItems(param: SearchGearPackItemsParam): Result<List<GearPackItemSearchResult>, AppError>
}
