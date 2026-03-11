package com.acme.clients.ingredientclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.ingredientclient.api.*
import com.acme.clients.ingredientclient.internal.operations.*
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi

internal class JdbiIngredientClient(jdbi: Jdbi) : IngredientClient {

    private val getIngredientById = GetIngredientById(jdbi)
    private val createIngredient = CreateIngredient(jdbi)
    private val getAllIngredients = GetAllIngredients(jdbi)
    private val updateIngredient = UpdateIngredient(jdbi, getIngredientById)
    private val findIngredientByName = FindIngredientByName(jdbi)
    private val findIngredientsByNames = FindIngredientsByNames(jdbi)
    private val createIngredientBatch = CreateIngredientBatch(jdbi)
    private val deleteIngredient = DeleteIngredient(jdbi)

    override fun create(param: CreateIngredientParam): Result<Ingredient, AppError> = createIngredient.execute(param)
    override fun getById(param: GetByIdParam): Result<Ingredient, AppError> = getIngredientById.execute(param)
    override fun getAll(): Result<List<Ingredient>, AppError> = getAllIngredients.execute()
    override fun update(param: UpdateIngredientParam): Result<Ingredient, AppError> = updateIngredient.execute(param)
    override fun findByName(param: FindByNameParam): Result<Ingredient?, AppError> = findIngredientByName.execute(param)
    override fun findByNames(param: FindByNamesParam): Result<List<Ingredient>, AppError> = findIngredientsByNames.execute(param)
    override fun delete(param: DeleteIngredientParam): Result<Unit, AppError> = deleteIngredient.execute(param)
    override fun createBatch(param: CreateBatchParam): Result<List<Ingredient>, AppError> = createIngredientBatch.execute(param)
}
