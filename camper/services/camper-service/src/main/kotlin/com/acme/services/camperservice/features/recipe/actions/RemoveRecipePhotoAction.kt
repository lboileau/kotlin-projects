package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetRecipePhotoByIdParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.RemoveRecipePhotoParam

/** Idempotent: a photo already gone is a 404 here only when it was never this recipe's. */
internal class RemoveRecipePhotoAction(
    private val recipeClient: RecipeClient,
    private val photoStore: RecipePhotoStore
) {
    fun execute(param: RemoveRecipePhotoParam): Result<Unit, RecipeError> {
        val photo = when (val result = recipeClient.getPhotoById(GetRecipePhotoByIdParam(param.photoId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(RecipeError.PhotoNotFound(param.photoId))
                else -> Result.Failure(RecipeError.Invalid("photo", result.error.message))
            }
        }
        if (photo.recipeId != param.recipeId) return Result.Failure(RecipeError.PhotoNotFound(param.photoId))
        return photoStore.remove(photo)
    }
}
