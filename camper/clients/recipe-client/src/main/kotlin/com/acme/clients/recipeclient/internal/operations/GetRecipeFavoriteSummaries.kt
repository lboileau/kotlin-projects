package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeFavoriteSummariesParam
import com.acme.clients.recipeclient.internal.adapters.RecipeFavoriteSummaryRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeFavoriteSummaries
import com.acme.clients.recipeclient.model.RecipeFavoriteSummary
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * The batched favourite aggregate — one query for many recipe ids, so the recipe
 * list endpoint stays O(1) queries no matter how long the list is.
 *
 * Two things this operation deliberately does:
 * - An empty `recipeIds` short-circuits to an empty list. An empty `IN ()` is a
 *   PostgreSQL syntax error, so the guard must come before the query.
 * - **Recipes with no favourites are absent from the result**, not zero-filled — a
 *   `GROUP BY` over an empty set yields no row. Callers must default a missing id to
 *   `favoriteCount = 0, favoritedByMe = false`.
 */
internal class GetRecipeFavoriteSummaries(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipeFavoriteSummaries::class.java)
    private val validate = ValidateGetRecipeFavoriteSummaries()

    fun execute(param: GetRecipeFavoriteSummariesParam): Result<List<RecipeFavoriteSummary>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.recipeIds.isEmpty()) return success(emptyList())

        logger.debug("Fetching favorite summaries recipeCount={} userId={}", param.recipeIds.size, param.userId)
        val entities = jdbi.withHandle<List<RecipeFavoriteSummary>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT recipe_id,
                       COUNT(*)::int              AS favorite_count,
                       BOOL_OR(user_id = :userId) AS favorited_by_me
                FROM recipe_favorites
                WHERE recipe_id IN (<recipeIds>)
                GROUP BY recipe_id
                """.trimIndent()
            )
                .bindList("recipeIds", param.recipeIds.distinct())
                .bind("userId", param.userId)
                .map { rs, _ -> RecipeFavoriteSummaryRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
