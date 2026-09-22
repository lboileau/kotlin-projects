package com.acme.clients.photostorageclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError

/**
 * Where photo bytes live. The service stores only a key per photo and never sees S3: in
 * production this is the Railway bucket, locally a folder, in tests a map.
 */
interface PhotoStorageClient {
    /** Stores (or overwrites) the object at the key. */
    fun put(param: PutObjectParam): Result<Unit, AppError>

    /** The object, or null if there is none at the key. */
    fun get(param: GetObjectParam): Result<StoredObject?, AppError>

    /** Removes the object; removing one that isn't there is a success. */
    fun delete(param: DeleteObjectParam): Result<Unit, AppError>

    /**
     * A URL a browser can load the object from with no credentials and no headers (an `<img>`
     * can send neither). Valid for at least [UrlForParam.minValiditySeconds] from now; the
     * same key asked for again soon after returns the same URL, so a page that refetches its
     * recipe doesn't make the browser re-download every photo.
     */
    fun urlFor(param: UrlForParam): Result<String, AppError>
}
