package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.photostorageclient.api.GetObjectParam
import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.api.StoredObject
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.GetStoredPhotoParam

/**
 * Serves an object by key — what the local file store's URLs point at. Keys are unguessable
 * (`recipes/{recipeId}/{photoId}.ext`), which is the same trust `GET /api/recipes/{id}` relies on.
 */
internal class GetStoredPhotoAction(
    private val storage: PhotoStorageClient
) {
    fun execute(param: GetStoredPhotoParam): Result<StoredObject, RecipeError> =
        when (val result = storage.get(GetObjectParam(param.key))) {
            is Result.Success -> result.value?.let { Result.Success(it) }
                ?: Result.Failure(RecipeError.Invalid("key", "no such photo"))
            is Result.Failure -> Result.Failure(RecipeError.StorageFailed(result.error.message))
        }
}
