package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeFavoritesParam
import com.acme.clients.recipeclient.internal.adapters.RecipeFavoriteRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateGetRecipeFavorites
import com.acme.clients.recipeclient.model.RecipeFavorite
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Lists who favourited a recipe, oldest first. Empty when nobody has.
 *
 * `id` is the tiebreaker in the ORDER BY so that rows inserted in the same
 * transaction — which share an identical `now()` for `created_at` — still come back
 * in a stable, repeatable order.
 */
internal class GetRecipeFavorites(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipeFavorites::class.java)
    private val validate = ValidateGetRecipeFavorites()

    fun execute(param: GetRecipeFavoritesParam): Result<List<RecipeFavorite>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching favorites for recipe id={}", param.recipeId)
        val entities = jdbi.withHandle<List<RecipeFavorite>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, recipe_id, user_id, created_at
                FROM recipe_favorites
                WHERE recipe_id = :recipeId
                ORDER BY created_at, id
                """.trimIndent()
            )
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> RecipeFavoriteRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
