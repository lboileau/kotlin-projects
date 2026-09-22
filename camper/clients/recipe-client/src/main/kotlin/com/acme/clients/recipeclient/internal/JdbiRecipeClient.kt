package com.acme.clients.recipeclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipeclient.api.*
import com.acme.clients.recipeclient.internal.operations.*
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeFavorite
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.clients.recipeclient.model.RecipeStep
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
    private val addRecipeFavorite = AddRecipeFavorite(jdbi)
    private val removeRecipeFavorite = RemoveRecipeFavorite(jdbi)
    private val getRecipeFavorites = GetRecipeFavorites(jdbi)
    private val getRecipeFavoriteSummaries = GetRecipeFavoriteSummaries(jdbi)
    private val getRecipeSteps = GetRecipeSteps(jdbi)
    private val replaceRecipeSteps = ReplaceRecipeSteps(jdbi)
    private val getRecipePhotos = GetRecipePhotos(jdbi)
    private val getRecipePhotoById = GetRecipePhotoById(jdbi)
    private val addRecipePhoto = AddRecipePhoto(jdbi)
    private val removeRecipePhoto = RemoveRecipePhoto(jdbi)

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

    override fun addFavorite(param: AddRecipeFavoriteParam): Result<Unit, AppError> = addRecipeFavorite.execute(param)
    override fun removeFavorite(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> = removeRecipeFavorite.execute(param)
    override fun getFavorites(param: GetRecipeFavoritesParam): Result<List<RecipeFavorite>, AppError> = getRecipeFavorites.execute(param)
    override fun getFavoriteSummaries(param: GetRecipeFavoriteSummariesParam): Result<List<RecipeFavoriteSummary>, AppError> = getRecipeFavoriteSummaries.execute(param)

    override fun getSteps(param: GetRecipeStepsParam): Result<List<RecipeStep>, AppError> = getRecipeSteps.execute(param)
    override fun replaceSteps(param: ReplaceRecipeStepsParam): Result<List<RecipeStep>, AppError> = replaceRecipeSteps.execute(param)
    override fun getPhotos(param: GetRecipePhotosParam): Result<List<RecipePhoto>, AppError> = getRecipePhotos.execute(param)
    override fun getPhotoById(param: GetRecipePhotoByIdParam): Result<RecipePhoto, AppError> = getRecipePhotoById.execute(param)
    override fun addPhoto(param: AddRecipePhotoParam): Result<RecipePhoto, AppError> = addRecipePhoto.execute(param)
    override fun removePhoto(param: RemoveRecipePhotoParam): Result<Unit, AppError> = removeRecipePhoto.execute(param)
}
