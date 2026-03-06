package com.acme.clients.worldclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.worldclient.model.World

/**
 * Client interface for World entity CRUD operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface WorldClient {
    /** Retrieve a single world by its unique identifier. */
    fun getById(param: GetByIdParam): Result<World, AppError>

    /** Retrieve a list of worlds, ordered by name. */
    fun getList(param: GetListParam): Result<List<World>, AppError>

    /** Create a new world with the given name and greeting. */
    fun create(param: CreateWorldParam): Result<World, AppError>

    /** Update an existing world. Null fields are left unchanged. */
    fun update(param: UpdateWorldParam): Result<World, AppError>

    /** Delete a world by its unique identifier. */
    fun delete(param: DeleteWorldParam): Result<Unit, AppError>
}
