package com.acme.clients.photostorageclient.api

data class PutObjectParam(
    /** Slash-separated path inside the bucket, e.g. `recipes/{recipeId}/{photoId}.jpg`. */
    val key: String,
    val mediaType: String,
    val bytes: ByteArray
)

data class GetObjectParam(val key: String)

data class DeleteObjectParam(val key: String)

data class UrlForParam(
    val key: String,
    /** How long the caller needs the URL to keep working. Default: an hour, a cooking session. */
    val minValiditySeconds: Long = 3600
)

class StoredObject(
    val mediaType: String,
    val bytes: ByteArray
)
