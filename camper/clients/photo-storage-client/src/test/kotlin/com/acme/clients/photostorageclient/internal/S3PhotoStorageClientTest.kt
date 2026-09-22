package com.acme.clients.photostorageclient.internal

import com.acme.clients.common.Result
import com.acme.clients.photostorageclient.api.DeleteObjectParam
import com.acme.clients.photostorageclient.api.GetObjectParam
import com.acme.clients.photostorageclient.api.PutObjectParam
import com.acme.clients.photostorageclient.api.UrlForParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** The S3 client against a real S3 API (MinIO), including the presigned-URL round trip a browser would make. */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3PhotoStorageClientTest {

    companion object {
        @Container
        @JvmStatic
        val minio: MinIOContainer = MinIOContainer(DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z.hotfix.7aa24e772").asCompatibleSubstituteFor("minio/minio"))
    }

    private val bucket = "camper-photos"
    private var now = 1_000_000_000_000L
    private var presignCalls = 0
    private lateinit var client: S3PhotoStorageClient

    @BeforeAll
    fun setUp() {
        val endpoint = URI.create(minio.s3URL)
        val credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(minio.userName, minio.password))
        // MinIO needs path-style; Railway's bucket is virtual-hosted. Both go through the same client.
        val config = S3Configuration.builder().pathStyleAccessEnabled(true).build()
        val s3 = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
            .credentialsProvider(credentials).serviceConfiguration(config).build()
        val real = S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
            .credentialsProvider(credentials).serviceConfiguration(config).build()
        // The presigner stamps wall-clock time into the URL, so two signings in the same second are
        // identical; what the memo tests need to see is whether it was asked at all.
        val counting = object : S3Presigner by real {
            override fun presignGetObject(request: GetObjectPresignRequest): PresignedGetObjectRequest {
                presignCalls += 1
                return real.presignGetObject(request)
            }
        }
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
        client = S3PhotoStorageClient(s3, counting, bucket, clock = { now })
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)

    @Test
    fun `put then get returns the bytes and content type`() {
        val key = "recipes/r1/p1.jpg"
        assertThat(client.put(PutObjectParam(key, "image/jpeg", jpeg)).isSuccess).isTrue()

        val got = (client.get(GetObjectParam(key)) as Result.Success).value
        assertThat(got).isNotNull
        assertThat(got!!.mediaType).isEqualTo("image/jpeg")
        assertThat(got.bytes).isEqualTo(jpeg)
    }

    @Test
    fun `get of a missing key is a success with null, and delete of one is a success`() {
        assertThat((client.get(GetObjectParam("recipes/none/none.jpg")) as Result.Success).value).isNull()
        assertThat(client.delete(DeleteObjectParam("recipes/none/none.jpg")).isSuccess).isTrue()
    }

    @Test
    fun `delete removes the object`() {
        val key = "recipes/r2/p2.jpg"
        client.put(PutObjectParam(key, "image/jpeg", jpeg))

        assertThat(client.delete(DeleteObjectParam(key)).isSuccess).isTrue()

        assertThat((client.get(GetObjectParam(key)) as Result.Success).value).isNull()
    }

    @Test
    fun `presigned url serves the object with no credentials`() {
        val key = "recipes/r3/p3.jpg"
        client.put(PutObjectParam(key, "image/jpeg", jpeg))

        val url = (client.urlFor(UrlForParam(key)) as Result.Success).value
        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()
        )

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).isEqualTo(jpeg)
        assertThat(response.headers().firstValue("content-type").orElse("")).isEqualTo("image/jpeg")
    }

    @Test
    fun `the same url is returned while the asked-for validity remains, then a fresh one`() {
        val key = "recipes/r4/p4.jpg"
        client.put(PutObjectParam(key, "image/jpeg", jpeg))

        val calls = presignCalls
        val first = (client.urlFor(UrlForParam(key, minValiditySeconds = 3600)) as Result.Success).value
        now += 50 * 60 * 1000 // 50 minutes later: 70 of the signed 120 remain, still ≥ 60 asked for
        val second = (client.urlFor(UrlForParam(key, minValiditySeconds = 3600)) as Result.Success).value
        assertThat(second).isEqualTo(first)
        assertThat(presignCalls).isEqualTo(calls + 1)

        now += 20 * 60 * 1000 // 70 minutes in: 50 remain, less than the hour asked for
        client.urlFor(UrlForParam(key, minValiditySeconds = 3600))
        assertThat(presignCalls).isEqualTo(calls + 2)
        assertThat(first).contains("X-Amz-Expires=7200")
    }

    @Test
    fun `deleting an object forgets its cached url`() {
        val key = "recipes/r5/p5.jpg"
        client.put(PutObjectParam(key, "image/jpeg", jpeg))
        client.urlFor(UrlForParam(key))
        val calls = presignCalls

        client.delete(DeleteObjectParam(key))
        client.put(PutObjectParam(key, "image/jpeg", jpeg))
        client.urlFor(UrlForParam(key))

        assertThat(presignCalls).isEqualTo(calls + 1)
    }
}
