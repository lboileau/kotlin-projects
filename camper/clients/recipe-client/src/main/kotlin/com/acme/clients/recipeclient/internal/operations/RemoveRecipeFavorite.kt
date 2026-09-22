package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.RemoveRecipeFavoriteParam
import com.acme.clients.recipeclient.internal.validations.ValidateRemoveRecipeFavorite
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Removes a user's favourite of a recipe.
 *
 * **Deliberately returns `success(Unit)` even when 0 rows were deleted.** Every other
 * delete in this client (see `DeleteRecipe`, `RemoveRecipeIngredient`) answers
 * `NotFoundError` on 0 affected rows; this one does not, because un-favouriting is
 * specified as idempotent — un-favouriting something that was never favourited is a
 * no-op, not an error. Do not "fix" this to match the others.
 */
internal class RemoveRecipeFavorite(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveRecipeFavorite::class.java)
    private val validate = ValidateRemoveRecipeFavorite()

    fun execute(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing favorite recipeId={} userId={}", param.recipeId, param.userId)
        val deleted = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                "DELETE FROM recipe_favorites WHERE recipe_id = :recipeId AND user_id = :userId"
            )
                .bind("recipeId", param.recipeId)
                .bind("userId", param.userId)
                .execute()
        }
        logger.debug("Removed favorite recipeId={} userId={} deleted={}", param.recipeId, param.userId, deleted)
        return success(Unit)
    }
}
