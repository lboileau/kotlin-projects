package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.services.camperservice.features.recipe.dto.RecipePhotoResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.AddRecipePhotoParam

/** A person adds a photo to a recipe. Immediate — not part of the edit form. */
internal class AddRecipePhotoAction(
    private val recipeClient: RecipeClient,
    private val photoStore: RecipePhotoStore
) {
    fun execute(param: AddRecipePhotoParam): Result<RecipePhotoResponse, RecipeError> {
        when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> {}
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }
        val existing = when (val result = photoStore.photos(param.recipeId)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }
        if (existing.size >= RecipePhotoStore.MAX_PHOTOS_PER_RECIPE) {
            return Result.Failure(RecipeError.PhotoLimit(param.recipeId, RecipePhotoStore.MAX_PHOTOS_PER_RECIPE))
        }
        val image = when (val result = photoStore.decode("photo", param.mediaType, param.data)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }
        val photo = when (val result = photoStore.store(param.recipeId, param.userId, image, RecipePhoto.SOURCE_UPLOAD, role = null)) {
            is Result.Success -> result.value
            is Result.Failure -> return result
        }
        return photoStore.toResponse(photo)
    }
}
