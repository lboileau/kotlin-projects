package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.RecipePhoto
import java.sql.ResultSet
import java.util.UUID

internal object RecipePhotoRowAdapter {
    const val COLUMNS = "id, recipe_id, position, storage_key, media_type, byte_size, width, height, source, role, created_by, created_at"

    fun fromResultSet(rs: ResultSet): RecipePhoto = RecipePhoto(
        id = rs.getObject("id", UUID::class.java),
        recipeId = rs.getObject("recipe_id", UUID::class.java),
        position = rs.getInt("position"),
        storageKey = rs.getString("storage_key"),
        mediaType = rs.getString("media_type"),
        byteSize = rs.getInt("byte_size"),
        width = rs.getObject("width", Integer::class.java)?.toInt(),
        height = rs.getObject("height", Integer::class.java)?.toInt(),
        source = rs.getString("source"),
        role = rs.getString("role"),
        createdBy = rs.getObject("created_by", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
