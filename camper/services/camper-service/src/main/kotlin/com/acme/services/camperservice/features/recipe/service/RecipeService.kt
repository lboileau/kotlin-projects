package com.acme.services.camperservice.features.recipe.service

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.recipe.actions.AddRecipeIngredientAction
import com.acme.services.camperservice.features.recipe.actions.AddRecipePhotoAction
import com.acme.services.camperservice.features.recipe.actions.GetStoredPhotoAction
import com.acme.services.camperservice.features.recipe.actions.RecipePhotoStore
import com.acme.services.camperservice.features.recipe.actions.RemoveRecipePhotoAction
import com.acme.services.camperservice.features.recipe.actions.ReplaceRecipeStepsAction
import com.acme.services.camperservice.features.recipe.actions.CreateRecipeAction
import com.acme.services.camperservice.features.recipe.actions.DeleteRecipeAction
import com.acme.services.camperservice.features.recipe.actions.FavoriteRecipeAction
import com.acme.services.camperservice.features.recipe.actions.GetRecipeAction
import com.acme.services.camperservice.features.recipe.actions.HtmlFetcher
import com.acme.services.camperservice.features.recipe.actions.ImportRecipeAction
import com.acme.services.camperservice.features.recipe.actions.ImportRecipeFromImagesAction
import com.acme.services.camperservice.features.recipe.actions.ListRecipeFavoritesAction
import com.acme.services.camperservice.features.recipe.actions.ListRecipesAction
import com.acme.services.camperservice.features.recipe.actions.PublishRecipeAction
import com.acme.services.camperservice.features.recipe.actions.RemoveRecipeIngredientAction
import com.acme.services.camperservice.features.recipe.actions.ResolveDuplicateAction
import com.acme.services.camperservice.features.recipe.actions.ResolveIngredientAction
import com.acme.services.camperservice.features.recipe.actions.UnfavoriteRecipeAction
import com.acme.services.camperservice.features.recipe.actions.UpdateRecipeAction
import com.acme.services.camperservice.features.recipe.actions.defaultHtmlFetcher
import com.acme.services.camperservice.features.recipe.params.AddRecipeIngredientParam
import com.acme.services.camperservice.features.recipe.params.AddRecipePhotoParam
import com.acme.services.camperservice.features.recipe.params.GetStoredPhotoParam
import com.acme.services.camperservice.features.recipe.params.RemoveRecipePhotoParam
import com.acme.services.camperservice.features.recipe.params.ReplaceRecipeStepsParam
import com.acme.services.camperservice.features.recipe.params.CreateRecipeParam
import com.acme.services.camperservice.features.recipe.params.DeleteRecipeParam
import com.acme.services.camperservice.features.recipe.params.FavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import com.acme.services.camperservice.features.recipe.params.ImportRecipeFromImagesParam
import com.acme.services.camperservice.features.recipe.params.ImportRecipeParam
import com.acme.services.camperservice.features.recipe.params.ListRecipeFavoritesParam
import com.acme.services.camperservice.features.recipe.params.ListRecipesParam
import com.acme.services.camperservice.features.recipe.params.PublishRecipeParam
import com.acme.services.camperservice.features.recipe.params.ResolveDuplicateParam
import com.acme.services.camperservice.features.recipe.params.RemoveRecipeIngredientParam
import com.acme.services.camperservice.features.recipe.params.ResolveIngredientParam
import com.acme.services.camperservice.features.recipe.params.UnfavoriteRecipeParam
import com.acme.services.camperservice.features.recipe.params.UpdateRecipeParam

class RecipeService(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    recipeScraperClient: RecipeScraperClient,
    userClient: UserClient,
    photoStorageClient: PhotoStorageClient,
    htmlFetcher: HtmlFetcher = defaultHtmlFetcher()
) {
    private val photoStore = RecipePhotoStore(recipeClient, photoStorageClient)
    private val addRecipeIngredient = AddRecipeIngredientAction(recipeClient, ingredientClient)
    private val createRecipe = CreateRecipeAction(recipeClient, ingredientClient)
    private val importRecipe = ImportRecipeAction(recipeClient, ingredientClient, recipeScraperClient, photoStore, htmlFetcher)
    private val importRecipeFromImages = ImportRecipeFromImagesAction(recipeClient, ingredientClient, recipeScraperClient, photoStore)
    private val getRecipe = GetRecipeAction(recipeClient, ingredientClient, photoStore)
    private val listRecipes = ListRecipesAction(recipeClient)
    private val updateRecipe = UpdateRecipeAction(recipeClient)
    private val deleteRecipe = DeleteRecipeAction(recipeClient, photoStore)
    private val replaceRecipeSteps = ReplaceRecipeStepsAction(recipeClient)
    private val addRecipePhoto = AddRecipePhotoAction(recipeClient, photoStore)
    private val removeRecipePhoto = RemoveRecipePhotoAction(recipeClient, photoStore)
    private val getStoredPhoto = GetStoredPhotoAction(photoStorageClient)
    private val resolveIngredient = ResolveIngredientAction(recipeClient, ingredientClient)
    private val resolveDuplicate = ResolveDuplicateAction(recipeClient)
    private val publishRecipe = PublishRecipeAction(recipeClient)
    private val removeRecipeIngredient = RemoveRecipeIngredientAction(recipeClient)
    private val favoriteRecipe = FavoriteRecipeAction(recipeClient)
    private val unfavoriteRecipe = UnfavoriteRecipeAction(recipeClient)
    private val listRecipeFavorites = ListRecipeFavoritesAction(recipeClient, userClient)

    fun create(param: CreateRecipeParam) = createRecipe.execute(param)
    fun import(param: ImportRecipeParam) = importRecipe.execute(param)
    fun importFromImages(param: ImportRecipeFromImagesParam) = importRecipeFromImages.execute(param)
    fun get(param: GetRecipeParam) = getRecipe.execute(param)
    fun list(param: ListRecipesParam) = listRecipes.execute(param)
    fun update(param: UpdateRecipeParam) = updateRecipe.execute(param)
    fun delete(param: DeleteRecipeParam) = deleteRecipe.execute(param)
    fun resolveIngredient(param: ResolveIngredientParam) = resolveIngredient.execute(param)
    fun resolveDuplicate(param: ResolveDuplicateParam) = resolveDuplicate.execute(param)
    fun publish(param: PublishRecipeParam) = publishRecipe.execute(param)
    fun removeIngredient(param: RemoveRecipeIngredientParam) = removeRecipeIngredient.execute(param)
    fun addIngredient(param: AddRecipeIngredientParam) = addRecipeIngredient.execute(param)
    fun favorite(param: FavoriteRecipeParam) = favoriteRecipe.execute(param)
    fun unfavorite(param: UnfavoriteRecipeParam) = unfavoriteRecipe.execute(param)
    fun listFavorites(param: ListRecipeFavoritesParam) = listRecipeFavorites.execute(param)
    fun replaceSteps(param: ReplaceRecipeStepsParam) = replaceRecipeSteps.execute(param)
    fun addPhoto(param: AddRecipePhotoParam) = addRecipePhoto.execute(param)
    fun removePhoto(param: RemoveRecipePhotoParam) = removeRecipePhoto.execute(param)
    fun getStoredPhoto(param: GetStoredPhotoParam) = getStoredPhoto.execute(param)
}
