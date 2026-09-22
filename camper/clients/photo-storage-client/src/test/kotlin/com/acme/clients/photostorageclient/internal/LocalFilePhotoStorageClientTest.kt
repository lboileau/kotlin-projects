package com.acme.clients.photostorageclient.internal

import com.acme.clients.common.Result
import com.acme.clients.photostorageclient.api.DeleteObjectParam
import com.acme.clients.photostorageclient.api.GetObjectParam
import com.acme.clients.photostorageclient.api.PutObjectParam
import com.acme.clients.photostorageclient.api.UrlForParam
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LocalFilePhotoStorageClientTest {

    @TempDir
    lateinit var dir: File

    private val bytes = byteArrayOf(1, 2, 3)

    @Test
    fun `round trip through files, media type from the extension, url under the public base`() {
        val client = LocalFilePhotoStorageClient(dir, "/api/photo-store/")
        val key = "recipes/abc/def.jpg"

        assertThat(client.put(PutObjectParam(key, "image/jpeg", bytes)).isSuccess).isTrue()
        assertThat(File(dir, key).readBytes()).isEqualTo(bytes)

        val got = (client.get(GetObjectParam(key)) as Result.Success).value!!
        assertThat(got.mediaType).isEqualTo("image/jpeg")
        assertThat(got.bytes).isEqualTo(bytes)
        assertThat((client.urlFor(UrlForParam(key)) as Result.Success).value).isEqualTo("/api/photo-store/recipes/abc/def.jpg")

        assertThat(client.delete(DeleteObjectParam(key)).isSuccess).isTrue()
        assertThat((client.get(GetObjectParam(key)) as Result.Success).value).isNull()
        assertThat(client.delete(DeleteObjectParam(key)).isSuccess).isTrue()
    }

    @Test
    fun `a key that could escape the directory is refused everywhere`() {
        val client = LocalFilePhotoStorageClient(dir, "/api/photo-store")
        listOf("../etc/passwd", "recipes/../../x.jpg", "/abs.jpg", "a b.jpg", "").forEach { key ->
            assertThat(client.put(PutObjectParam(key, "image/jpeg", bytes)).isFailure).`as`(key).isTrue()
            assertThat(client.get(GetObjectParam(key)).isFailure).`as`(key).isTrue()
            assertThat(client.urlFor(UrlForParam(key)).isFailure).`as`(key).isTrue()
        }
        assertThat(dir.listFiles()).isEmpty()
    }

    @Test
    fun `media type falls back for unknown extensions`() {
        assertThat(LocalFilePhotoStorageClient.mediaTypeFor("a/b.PNG")).isEqualTo("image/png")
        assertThat(LocalFilePhotoStorageClient.mediaTypeFor("a/b.webp")).isEqualTo("image/webp")
        assertThat(LocalFilePhotoStorageClient.mediaTypeFor("a/b")).isEqualTo("application/octet-stream")
    }
}
