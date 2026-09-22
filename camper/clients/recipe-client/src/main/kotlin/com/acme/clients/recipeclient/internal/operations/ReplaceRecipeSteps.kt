package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.ReplaceRecipeStepsParam
import com.acme.clients.recipeclient.internal.adapters.RecipeStepRowAdapter
import com.acme.clients.recipeclient.internal.validations.ValidateReplaceRecipeSteps
import com.acme.clients.recipeclient.model.RecipeStep
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/** Delete-then-insert in one transaction: the list is the unit, so there is nothing to merge. */
internal class ReplaceRecipeSteps(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(ReplaceRecipeSteps::class.java)
    private val validate = ValidateReplaceRecipeSteps()

    fun execute(param: ReplaceRecipeStepsParam): Result<List<RecipeStep>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Replacing steps of recipe id={} with {} step(s)", param.recipeId, param.texts.size)
        val steps = jdbi.inTransaction<List<RecipeStep>, Exception> { handle ->
            handle.createUpdate("DELETE FROM recipe_steps WHERE recipe_id = :recipeId")
                .bind("recipeId", param.recipeId)
                .execute()
            if (param.texts.isEmpty()) return@inTransaction emptyList()

            val now = Instant.now()
            val batch = handle.prepareBatch(
                "INSERT INTO recipe_steps (id, recipe_id, position, text, created_at) VALUES (:id, :recipeId, :position, :text, :createdAt)"
            )
            param.texts.forEachIndexed { position, text ->
                batch.bind("id", UUID.randomUUID())
                    .bind("recipeId", param.recipeId)
                    .bind("position", position)
                    .bind("text", text.trim())
                    .bind("createdAt", now)
                    .add()
            }
            batch.execute()

            handle.createQuery("SELECT ${RecipeStepRowAdapter.COLUMNS} FROM recipe_steps WHERE recipe_id = :recipeId ORDER BY position")
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> RecipeStepRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(steps)
    }
}
