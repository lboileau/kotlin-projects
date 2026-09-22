package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts one row of the batched favourite aggregate to [RecipeFavoriteSummary].
 *
 * `favorite_count` is cast to `int` in the SQL (a bare `COUNT(*)` is `bigint`,
 * which `getInt` would narrow silently), and `favorited_by_me` is a `BOOL_OR`
 * over a non-empty group, so neither column is ever NULL.
 */
internal object RecipeFavoriteSummaryRowAdapter {

    fun fromResultSet(rs: ResultSet): RecipeFavoriteSummary = RecipeFavoriteSummary(
        recipeId = rs.getObject("recipe_id", UUID::class.java),
        favoriteCount = rs.getInt("favorite_count"),
        favoritedByMe = rs.getBoolean("favorited_by_me")
    )
}
