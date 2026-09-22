package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.RecipeFavorite
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts `recipe_favorites` rows to [RecipeFavorite] domain objects.
 * The table has no `updated_at` — rows are only inserted and deleted.
 */
internal object RecipeFavoriteRowAdapter {

    fun fromResultSet(rs: ResultSet): RecipeFavorite = RecipeFavorite(
        id = rs.getObject("id", UUID::class.java),
        recipeId = rs.getObject("recipe_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
