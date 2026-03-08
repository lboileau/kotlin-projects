package com.acme.clients.itemclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itemclient.api.*
import com.acme.clients.itemclient.model.Item

class FakeItemClient : ItemClient {

    override fun create(param: CreateItemParam): Result<Item, AppError> {
        throw NotImplementedError()
    }

    override fun getById(param: GetByIdParam): Result<Item, AppError> {
        throw NotImplementedError()
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Item>, AppError> {
        throw NotImplementedError()
    }

    override fun getByUserId(param: GetByUserIdParam): Result<List<Item>, AppError> {
        throw NotImplementedError()
    }

    override fun update(param: UpdateItemParam): Result<Item, AppError> {
        throw NotImplementedError()
    }

    override fun delete(param: DeleteItemParam): Result<Unit, AppError> {
        throw NotImplementedError()
    }

    fun reset() {
        // No-op for stub
    }
}
