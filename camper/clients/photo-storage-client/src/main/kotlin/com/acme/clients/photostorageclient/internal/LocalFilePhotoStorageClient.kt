package com.acme.clients.photostorageclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.photostorageclient.api.DeleteObjectParam
import com.acme.clients.photostorageclient.api.GetObjectParam
import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.api.PutObjectParam
import com.acme.clients.photostorageclient.api.StoredObject
import com.acme.clients.photostorageclient.api.UrlForParam
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Local development with no bucket: objects are files under [dir] (keyed by their path), and a
 * URL is [publicBaseUrl]/key — a route the service serves from this same store. Keys are
 * validated so a key can never escape the directory.
 */
internal class LocalFilePhotoStorageClient(
    private val dir: File,
    private val publicBaseUrl: String
) : PhotoStorageClient {
    private val logger = LoggerFactory.getLogger(LocalFilePhotoStorageClient::class.java)

    override fun put(param: PutObjectParam): Result<Unit, AppError> = withFile(param.key) { file ->
        file.parentFile.mkdirs()
        file.writeBytes(param.bytes)
        success(Unit)
    }

    override fun get(param: GetObjectParam): Result<StoredObject?, AppError> = withFile(param.key) { file ->
        if (!file.isFile) success(null)
        else success(StoredObject(mediaType = mediaTypeFor(param.key), bytes = file.readBytes()))
    }

    override fun delete(param: DeleteObjectParam): Result<Unit, AppError> = withFile(param.key) { file ->
        file.delete()
        success(Unit)
    }

    override fun urlFor(param: UrlForParam): Result<String, AppError> =
        if (isSafe(param.key)) success("${publicBaseUrl.trimEnd('/')}/${param.key}")
        else failure(InternalError("Invalid photo key"))

    private inline fun <T> withFile(key: String, block: (File) -> Result<T, AppError>): Result<T, AppError> {
        if (!isSafe(key)) return failure(InternalError("Invalid photo key"))
        return try {
            block(File(dir, key))
        } catch (e: Exception) {
            logger.error("Local photo store failed for {}: {}", key, e.message)
            failure(InternalError("Photo storage failed: ${e.message}"))
        }
    }

    companion object {
        private val SAFE_KEY = Regex("""^[A-Za-z0-9_\-]+(/[A-Za-z0-9_\-]+)*(\.[A-Za-z0-9]+)?$""")

        fun isSafe(key: String) = SAFE_KEY.matches(key)

        /** Local files carry no content type, so it comes from the key's extension. */
        fun mediaTypeFor(key: String): String = when (key.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}
