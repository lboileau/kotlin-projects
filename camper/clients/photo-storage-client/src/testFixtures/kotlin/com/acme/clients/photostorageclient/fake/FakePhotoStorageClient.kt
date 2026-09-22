package com.acme.clients.photostorageclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.photostorageclient.api.DeleteObjectParam
import com.acme.clients.photostorageclient.api.GetObjectParam
import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.api.PutObjectParam
import com.acme.clients.photostorageclient.api.StoredObject
import com.acme.clients.photostorageclient.api.UrlForParam
import java.util.concurrent.ConcurrentHashMap

class FakePhotoStorageClient : PhotoStorageClient {
    private val objects = ConcurrentHashMap<String, StoredObject>()

    /** Set to make the next call of that kind fail, e.g. to test cleanup on a storage error. */
    var nextPutResult: Result<Unit, AppError>? = null
    var nextDeleteResult: Result<Unit, AppError>? = null

    override fun put(param: PutObjectParam): Result<Unit, AppError> {
        nextPutResult?.let { nextPutResult = null; return it }
        objects[param.key] = StoredObject(param.mediaType, param.bytes)
        return success(Unit)
    }

    override fun get(param: GetObjectParam): Result<StoredObject?, AppError> = success(objects[param.key])

    override fun delete(param: DeleteObjectParam): Result<Unit, AppError> {
        nextDeleteResult?.let { nextDeleteResult = null; return it }
        objects.remove(param.key)
        return success(Unit)
    }

    override fun urlFor(param: UrlForParam): Result<String, AppError> = success("fake://${param.key}")

    fun keys(): Set<String> = objects.keys.toSet()
    fun has(key: String) = objects.containsKey(key)

    fun reset() {
        objects.clear()
        nextPutResult = null
        nextDeleteResult = null
    }
}
