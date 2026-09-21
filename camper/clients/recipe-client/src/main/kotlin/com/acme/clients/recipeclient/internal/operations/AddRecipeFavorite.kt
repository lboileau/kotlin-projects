package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.AddRecipeFavoriteParam
import com.acme.clients.recipeclient.internal.validations.ValidateAddRecipeFavorite
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Favourites a recipe for a user.
 *
 * Idempotency is the database's job: `ON CONFLICT (recipe_id, user_id) DO NOTHING`
 * means `uq_recipe_favorites_recipe_user` never raises, and re-favouriting leaves the
 * existing row — and therefore its `created_at` — untouched, so the ordering of the
 * "favourited by" list stays stable.
 *
 * Returns `success(Unit)` regardless of the affected row count. `id` and `created_at`
 * come from their column defaults.
 */
internal class AddRecipeFavorite(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipeFavorite::class.java)
    private val validate = ValidateAddRecipeFavorite()

    fun execute(param: AddRecipeFavoriteParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding favorite recipeId={} userId={}", param.recipeId, param.userId)
        val inserted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO recipe_favorites (recipe_id, user_id)
                VALUES (:recipeId, :userId)
                ON CONFLICT (recipe_id, user_id) DO NOTHING
                """.trimIndent()
            )
                .bind("recipeId", param.recipeId)
                .bind("userId", param.userId)
                .execute()
        }
        logger.debug("Added favorite recipeId={} userId={} inserted={}", param.recipeId, param.userId, inserted > 0)
        return success(Unit)
    }
}
