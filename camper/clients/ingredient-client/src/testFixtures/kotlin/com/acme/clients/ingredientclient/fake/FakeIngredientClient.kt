package com.acme.clients.ingredientclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.ingredientclient.api.*
import com.acme.clients.ingredientclient.model.Ingredient

class FakeIngredientClient : IngredientClient {

    override fun create(param: CreateIngredientParam): Result<Ingredient, AppError> {
        throw NotImplementedError("FakeIngredientClient.create not yet implemented")
    }

    override fun getById(param: GetByIdParam): Result<Ingredient, AppError> {
        throw NotImplementedError("FakeIngredientClient.getById not yet implemented")
    }

    override fun getAll(): Result<List<Ingredient>, AppError> {
        throw NotImplementedError("FakeIngredientClient.getAll not yet implemented")
    }

    override fun update(param: UpdateIngredientParam): Result<Ingredient, AppError> {
        throw NotImplementedError("FakeIngredientClient.update not yet implemented")
    }

    override fun findByName(param: FindByNameParam): Result<Ingredient?, AppError> {
        throw NotImplementedError("FakeIngredientClient.findByName not yet implemented")
    }

    override fun findByNames(param: FindByNamesParam): Result<List<Ingredient>, AppError> {
        throw NotImplementedError("FakeIngredientClient.findByNames not yet implemented")
    }

    override fun createBatch(param: CreateBatchParam): Result<List<Ingredient>, AppError> {
        throw NotImplementedError("FakeIngredientClient.createBatch not yet implemented")
    }
}
