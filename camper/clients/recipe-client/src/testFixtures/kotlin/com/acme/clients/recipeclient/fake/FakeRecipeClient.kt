package com.acme.clients.recipeclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipeclient.api.*
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient

class FakeRecipeClient : RecipeClient {

    override fun create(param: CreateRecipeParam): Result<Recipe, AppError> {
        throw NotImplementedError("FakeRecipeClient.create not yet implemented")
    }

    override fun getById(param: GetByIdParam): Result<Recipe, AppError> {
        throw NotImplementedError("FakeRecipeClient.getById not yet implemented")
    }

    override fun getAll(param: GetAllParam): Result<List<Recipe>, AppError> {
        throw NotImplementedError("FakeRecipeClient.getAll not yet implemented")
    }

    override fun update(param: UpdateRecipeParam): Result<Recipe, AppError> {
        throw NotImplementedError("FakeRecipeClient.update not yet implemented")
    }

    override fun delete(param: DeleteRecipeParam): Result<Unit, AppError> {
        throw NotImplementedError("FakeRecipeClient.delete not yet implemented")
    }

    override fun findByWebLink(param: FindByWebLinkParam): Result<Recipe?, AppError> {
        throw NotImplementedError("FakeRecipeClient.findByWebLink not yet implemented")
    }

    override fun findSimilarByName(param: FindSimilarParam): Result<List<Recipe>, AppError> {
        throw NotImplementedError("FakeRecipeClient.findSimilarByName not yet implemented")
    }

    override fun addIngredient(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        throw NotImplementedError("FakeRecipeClient.addIngredient not yet implemented")
    }

    override fun addIngredients(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        throw NotImplementedError("FakeRecipeClient.addIngredients not yet implemented")
    }

    override fun getIngredients(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> {
        throw NotImplementedError("FakeRecipeClient.getIngredients not yet implemented")
    }

    override fun updateIngredient(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError> {
        throw NotImplementedError("FakeRecipeClient.updateIngredient not yet implemented")
    }

    override fun removeIngredient(param: RemoveRecipeIngredientParam): Result<Unit, AppError> {
        throw NotImplementedError("FakeRecipeClient.removeIngredient not yet implemented")
    }
}
