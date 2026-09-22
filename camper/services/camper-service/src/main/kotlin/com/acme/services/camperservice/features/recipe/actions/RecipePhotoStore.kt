package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.photostorageclient.api.DeleteObjectParam
import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.api.PutObjectParam
import com.acme.clients.photostorageclient.api.UrlForParam
import com.acme.clients.recipeclient.api.AddRecipePhotoParam
import com.acme.clients.recipeclient.api.GetRecipePhotosParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.api.RemoveRecipePhotoParam
import com.acme.clients.recipeclient.model.RecipePhoto
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.services.camperservice.features.recipe.dto.RecipePhotoResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

/** A photo the request carried, checked and decoded once; what both storing and scraping consume. */
internal class DecodedImage(
    val mediaType: String,
    val base64: String,
    val bytes: ByteArray,
    val width: Int?,
    val height: Int?
)

/**
 * The photo half every action shares: checking an uploaded image, storing its bytes then its
 * row (and undoing the bytes if the row fails), deleting object-then-row, and turning a row into
 * the response with a URL a browser can load.
 */
internal class RecipePhotoStore(
    private val recipeClient: RecipeClient,
    private val storage: PhotoStorageClient
) {
    private val logger = LoggerFactory.getLogger(RecipePhotoStore::class.java)

    /**
     * Validates one uploaded image. [field] names it in errors (`images[0]`, `photo`). The size
     * check runs on the base64 length before decoding so an oversized upload is never decoded.
     * Dimensions come from ImageIO when it can read the format (JPEG/PNG/GIF; not WebP) and are
     * null otherwise — the photo is still accepted.
     */
    fun decode(field: String, mediaType: String, data: String): Result<DecodedImage, RecipeError> {
        val type = mediaType.trim().lowercase()
        if (type !in RecipeImage.SUPPORTED_MEDIA_TYPES) {
            return Result.Failure(RecipeError.Invalid("$field.mediaType", "must be one of ${RecipeImage.SUPPORTED_MEDIA_TYPES.sorted().joinToString()}"))
        }
        val base64 = data.trim()
        if (base64.isEmpty()) return Result.Failure(RecipeError.Invalid("$field.data", "must not be blank"))
        if (base64.startsWith("data:")) return Result.Failure(RecipeError.Invalid("$field.data", "must be raw base64, not a data: URL"))
        if (base64.length > MAX_IMAGE_BYTES / 3 * 4 + 4) {
            return Result.Failure(RecipeError.Invalid("$field.data", "photo is larger than ${MAX_IMAGE_BYTES / 1024 / 1024} MB"))
        }
        val bytes = try {
            Base64.getDecoder().decode(base64)
        } catch (e: IllegalArgumentException) {
            return Result.Failure(RecipeError.Invalid("$field.data", "is not valid base64"))
        }
        if (bytes.size > MAX_IMAGE_BYTES) {
            return Result.Failure(RecipeError.Invalid("$field.data", "photo is larger than ${MAX_IMAGE_BYTES / 1024 / 1024} MB"))
        }
        if (bytes.isEmpty()) return Result.Failure(RecipeError.Invalid("$field.data", "must not be empty"))
        val dimensions = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()?.let { it.width to it.height }
        return Result.Success(DecodedImage(type, base64, bytes, dimensions?.first, dimensions?.second))
    }

    /** Bytes to the store, then the row. If the row can't be written the object is removed again. */
    fun store(recipeId: UUID, userId: UUID, image: DecodedImage, source: String, role: String?): Result<RecipePhoto, RecipeError> {
        val id = UUID.randomUUID()
        val key = "recipes/$recipeId/$id.${extensionFor(image.mediaType)}"
        when (val put = storage.put(PutObjectParam(key, image.mediaType, image.bytes))) {
            is Result.Failure -> return Result.Failure(RecipeError.StorageFailed(put.error.message))
            is Result.Success -> {}
        }
        return when (val added = recipeClient.addPhoto(AddRecipePhotoParam(
            id = id, recipeId = recipeId, storageKey = key, mediaType = image.mediaType, byteSize = image.bytes.size,
            width = image.width, height = image.height, source = source, role = role, createdBy = userId
        ))) {
            is Result.Success -> Result.Success(added.value)
            is Result.Failure -> {
                storage.delete(DeleteObjectParam(key))
                Result.Failure(RecipeError.Invalid("photo", added.error.message))
            }
        }
    }

    /** Object first, then the row: a row without an object is a broken image; an object without a row is only wasted space. */
    fun remove(photo: RecipePhoto): Result<Unit, RecipeError> {
        when (val deleted = storage.delete(DeleteObjectParam(photo.storageKey))) {
            is Result.Failure -> return Result.Failure(RecipeError.StorageFailed(deleted.error.message))
            is Result.Success -> {}
        }
        return when (val removed = recipeClient.removePhoto(RemoveRecipePhotoParam(photo.id))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> when (removed.error) {
                is NotFoundError -> Result.Success(Unit)
                else -> Result.Failure(RecipeError.Invalid("photo", removed.error.message))
            }
        }
    }

    /** Every object a recipe owns, before the recipe row (and its photo rows, by cascade) goes. */
    fun removeAllObjects(recipeId: UUID): Result<Unit, RecipeError> {
        val photos = when (val result = recipeClient.getPhotos(GetRecipePhotosParam(recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("photos", result.error.message))
        }
        photos.forEach { photo ->
            when (val deleted = storage.delete(DeleteObjectParam(photo.storageKey))) {
                is Result.Failure -> return Result.Failure(RecipeError.StorageFailed(deleted.error.message))
                is Result.Success -> {}
            }
        }
        return Result.Success(Unit)
    }

    fun photos(recipeId: UUID): Result<List<RecipePhoto>, RecipeError> =
        when (val result = recipeClient.getPhotos(GetRecipePhotosParam(recipeId))) {
            is Result.Success -> Result.Success(result.value)
            is Result.Failure -> Result.Failure(RecipeError.Invalid("photos", result.error.message))
        }

    fun toResponse(photo: RecipePhoto): Result<RecipePhotoResponse, RecipeError> {
        val url = when (val result = storage.urlFor(UrlForParam(photo.storageKey))) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.error("No URL for photo {} ({}): {}", photo.id, photo.storageKey, result.error.message)
                return Result.Failure(RecipeError.StorageFailed(result.error.message))
            }
        }
        return Result.Success(
            RecipePhotoResponse(
                id = photo.id, url = url, mediaType = photo.mediaType, width = photo.width, height = photo.height,
                byteSize = photo.byteSize, source = photo.source, role = photo.role, position = photo.position, createdAt = photo.createdAt
            )
        )
    }

    fun toResponses(photos: List<RecipePhoto>): Result<List<RecipePhotoResponse>, RecipeError> {
        val out = ArrayList<RecipePhotoResponse>(photos.size)
        for (photo in photos) {
            when (val result = toResponse(photo)) {
                is Result.Success -> out.add(result.value)
                is Result.Failure -> return result
            }
        }
        return Result.Success(out)
    }

    companion object {
        /** The Claude API's per-image limit, and a sane cap for a stored photo. */
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        const val MAX_PHOTOS_PER_RECIPE = 6

        fun extensionFor(mediaType: String): String = when (mediaType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "bin"
        }
    }
}
