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
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Any S3-compatible bucket (Railway's, MinIO in tests). The bucket is private, so reads from a
 * browser go through presigned URLs.
 */
internal class S3PhotoStorageClient(
    private val s3: S3Client,
    private val presigner: S3Presigner,
    private val bucket: String,
    private val clock: () -> Long = System::currentTimeMillis
) : PhotoStorageClient {
    private val logger = LoggerFactory.getLogger(S3PhotoStorageClient::class.java)

    private class SignedUrl(val url: String, val expiresAtMillis: Long)

    /**
     * Presigned URLs are signed for twice the validity asked for and reused while at least the
     * asked-for validity remains, so repeated reads of the same recipe within an hour get the
     * same URL and the browser cache holds. Bounded by the number of photos ever served.
     */
    private val signed = ConcurrentHashMap<String, SignedUrl>()

    override fun put(param: PutObjectParam): Result<Unit, AppError> = guarded("put ${param.key}") {
        s3.putObject(
            PutObjectRequest.builder().bucket(bucket).key(param.key).contentType(param.mediaType).build(),
            RequestBody.fromBytes(param.bytes)
        )
        success(Unit)
    }

    override fun get(param: GetObjectParam): Result<StoredObject?, AppError> = guarded("get ${param.key}") {
        try {
            val bytes = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(param.key).build())
            success(StoredObject(mediaType = bytes.response().contentType() ?: "application/octet-stream", bytes = bytes.asByteArray()))
        } catch (e: NoSuchKeyException) {
            success(null)
        }
    }

    override fun delete(param: DeleteObjectParam): Result<Unit, AppError> = guarded("delete ${param.key}") {
        // S3 deletes are idempotent: a missing key is a 204 like any other.
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(param.key).build())
        signed.remove(param.key)
        success(Unit)
    }

    override fun urlFor(param: UrlForParam): Result<String, AppError> = guarded("presign ${param.key}") {
        val now = clock()
        val cached = signed[param.key]
        if (cached != null && cached.expiresAtMillis - now >= param.minValiditySeconds * 1000) {
            return@guarded success(cached.url)
        }
        val duration = Duration.ofSeconds(param.minValiditySeconds * 2)
        val presigned = presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(param.key).build())
                .build()
        )
        val url = presigned.url().toString()
        signed[param.key] = SignedUrl(url, now + duration.toMillis())
        success(url)
    }

    private inline fun <T> guarded(what: String, block: () -> Result<T, AppError>): Result<T, AppError> = try {
        block()
    } catch (e: Exception) {
        logger.error("Photo storage failed to {}: {}", what, e.message)
        failure(InternalError("Photo storage failed: ${e.message}"))
    }
}
