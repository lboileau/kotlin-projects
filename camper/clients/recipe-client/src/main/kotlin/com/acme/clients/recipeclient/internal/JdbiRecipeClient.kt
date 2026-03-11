package com.acme.clients.recipeclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipeclient.api.*
import com.acme.clients.recipeclient.internal.operations.*
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient
import org.jdbi.v3.core.Jdbi

internal class JdbiRecipeClient(jdbi: Jdbi) : RecipeClient {

    private val getRecipeById = GetRecipeById(jdbi)
    private val createRecipe = CreateRecipe(jdbi)
    private val getAllRecipes = GetAllRecipes(jdbi)
    private val updateRecipe = UpdateRecipe(jdbi, getRecipeById)
    private val deleteRecipe = DeleteRecipe(jdbi)
    private val findRecipeByWebLink = FindRecipeByWebLink(jdbi)
    private val findSimilarRecipes = FindSimilarRecipes(jdbi)
    private val addRecipeIngredient = AddRecipeIngredient(jdbi)
    private val addRecipeIngredients = AddRecipeIngredients(jdbi)
    private val getRecipeIngredients = GetRecipeIngredients(jdbi)
    private val updateRecipeIngredient = UpdateRecipeIngredient(jdbi)
    private val removeRecipeIngredient = RemoveRecipeIngredient(jdbi)
    private val findRecipeIngredientsByIngredientId = FindRecipeIngredientsByIngredientId(jdbi)

    override fun create(param: CreateRecipeParam): Result<Recipe, AppError> = createRecipe.execute(param)
    override fun getById(param: GetByIdParam): Result<Recipe, AppError> = getRecipeById.execute(param)
    override fun getAll(param: GetAllParam): Result<List<Recipe>, AppError> = getAllRecipes.execute(param)
    override fun update(param: UpdateRecipeParam): Result<Recipe, AppError> = updateRecipe.execute(param)
    override fun delete(param: DeleteRecipeParam): Result<Unit, AppError> = deleteRecipe.execute(param)
    override fun findByWebLink(param: FindByWebLinkParam): Result<Recipe?, AppError> = findRecipeByWebLink.execute(param)
    override fun findSimilarByName(param: FindSimilarParam): Result<List<Recipe>, AppError> = findSimilarRecipes.execute(param)
    override fun addIngredient(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError> = addRecipeIngredient.execute(param)
    override fun addIngredients(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> = addRecipeIngredients.execute(param)
    override fun getIngredients(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError> = getRecipeIngredients.execute(param)
    override fun updateIngredient(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError> = updateRecipeIngredient.execute(param)
    override fun removeIngredient(param: RemoveRecipeIngredientParam): Result<Unit, AppError> = removeRecipeIngredient.execute(param)
    override fun findIngredientsByIngredientId(param: FindRecipeIngredientsByIngredientIdParam): Result<List<RecipeIngredient>, AppError> = findRecipeIngredientsByIngredientId.execute(param)
}
