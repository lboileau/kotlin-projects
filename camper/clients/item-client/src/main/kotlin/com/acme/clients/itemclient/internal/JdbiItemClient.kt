package com.acme.clients.itemclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itemclient.api.*
import com.acme.clients.itemclient.internal.operations.*
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiItemClient(jdbi: Jdbi) : ItemClient {

    private val getItemById = GetItemById(jdbi)
    private val getItemsByPlanId = GetItemsByPlanId(jdbi)
    private val getItemsByUserId = GetItemsByUserId(jdbi)
    private val getItemsByPlanIdAndUserId = GetItemsByPlanIdAndUserId(jdbi)
    private val createItem = CreateItem(jdbi)
    private val updateItem = UpdateItem(jdbi, getItemById)
    private val deleteItem = DeleteItem(jdbi)

    override fun create(param: CreateItemParam): Result<Item, AppError> = createItem.execute(param)
    override fun getById(param: GetByIdParam): Result<Item, AppError> = getItemById.execute(param)
    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Item>, AppError> = getItemsByPlanId.execute(param)
    override fun getByUserId(param: GetByUserIdParam): Result<List<Item>, AppError> = getItemsByUserId.execute(param)
    override fun getByPlanIdAndUserId(param: GetByPlanIdAndUserIdParam): Result<List<Item>, AppError> = getItemsByPlanIdAndUserId.execute(param)
    override fun update(param: UpdateItemParam): Result<Item, AppError> = updateItem.execute(param)
    override fun delete(param: DeleteItemParam): Result<Unit, AppError> = deleteItem.execute(param)
}
