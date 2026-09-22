package com.acme.clients.photostorageclient

import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.internal.LocalFilePhotoStorageClient
import com.acme.clients.photostorageclient.internal.S3PhotoStorageClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.net.URI

/**
 * The bucket, from the standard AWS names (set on the Railway service; the SDK's own default
 * chain would find the credentials but not the endpoint or `AWS_DEFAULT_REGION`, so all five
 * are read here and a missing one fails at startup by name):
 * - AWS_S3_BUCKET_NAME, AWS_ENDPOINT_URL, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_DEFAULT_REGION
 * - AWS_S3_PATH_STYLE (optional, `true` for buckets that need path-style URLs; Railway's default is virtual-hosted)
 */
fun createS3PhotoStorageClient(): PhotoStorageClient {
    val bucket = required("AWS_S3_BUCKET_NAME")
    val endpoint = URI.create(required("AWS_ENDPOINT_URL"))
    val region = Region.of(required("AWS_DEFAULT_REGION"))
    val credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(required("AWS_ACCESS_KEY_ID"), required("AWS_SECRET_ACCESS_KEY"))
    )
    val pathStyle = setting("AWS_S3_PATH_STYLE")?.equals("true", ignoreCase = true) ?: false
    val serviceConfiguration = S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build()

    val s3 = S3Client.builder()
        .endpointOverride(endpoint)
        .region(region)
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfiguration)
        .build()
    val presigner = S3Presigner.builder()
        .endpointOverride(endpoint)
        .region(region)
        .credentialsProvider(credentials)
        .serviceConfiguration(serviceConfiguration)
        .build()
    return S3PhotoStorageClient(s3, presigner, bucket)
}

/** Local development: files under [dir], served by the service at [publicBaseUrl]/key. */
fun createLocalPhotoStorageClient(dir: File, publicBaseUrl: String): PhotoStorageClient =
    LocalFilePhotoStorageClient(dir, publicBaseUrl)

/** Whether the bucket is configured — the service picks the local store otherwise. */
fun isS3PhotoStorageConfigured(): Boolean = !setting("AWS_S3_BUCKET_NAME").isNullOrBlank()

private fun setting(name: String): String? = System.getProperty(name) ?: System.getenv(name)

private fun required(name: String): String =
    setting(name)?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("$name must be set")
