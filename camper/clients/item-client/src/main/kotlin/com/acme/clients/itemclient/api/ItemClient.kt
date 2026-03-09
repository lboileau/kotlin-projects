package com.acme.clients.itemclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itemclient.model.Item

/**
 * Client interface for Item entity CRUD operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface ItemClient {
    /** Create a new item. */
    fun create(param: CreateItemParam): Result<Item, AppError>

    /** Retrieve an item by its unique identifier. */
    fun getById(param: GetByIdParam): Result<Item, AppError>

    /** Retrieve all items belonging to a plan. */
    fun getByPlanId(param: GetByPlanIdParam): Result<List<Item>, AppError>

    /** Retrieve all items belonging to a user. */
    fun getByUserId(param: GetByUserIdParam): Result<List<Item>, AppError>

    /** Retrieve personal items for a user within a specific plan. */
    fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<List<Item>, AppError>

    /** Update an existing item. */
    fun update(param: UpdateItemParam): Result<Item, AppError>

    /** Delete an item by its unique identifier. */
    fun delete(param: DeleteItemParam): Result<Unit, AppError>
}
