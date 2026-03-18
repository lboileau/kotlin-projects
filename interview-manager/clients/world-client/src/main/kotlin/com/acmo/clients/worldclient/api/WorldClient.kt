package com.acmo.clients.worldclient.api

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.worldclient.model.World

/**
 * Client interface for world entity CRUD operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface WorldClient {
    /** Retrieve a single world by its unique identifier. */
    fun getById(param: GetByIdParam): Result<World, AppError>

    /** Retrieve a list of all worlds, ordered by name. */
    fun getList(param: GetListParam): Result<List<World>, AppError>

    /** Create a new world with the given fields. */
    fun create(param: CreateWorldParam): Result<World, AppError>

    /** Update an existing world. Null fields are left unchanged. */
    fun update(param: UpdateWorldParam): Result<World, AppError>

    /** Delete a world by its unique identifier. */
    fun delete(param: DeleteWorldParam): Result<Unit, AppError>
}
