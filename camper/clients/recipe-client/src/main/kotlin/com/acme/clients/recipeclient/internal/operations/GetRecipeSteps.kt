package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.GetRecipeStepsParam
import com.acme.clients.recipeclient.internal.adapters.RecipeStepRowAdapter
import com.acme.clients.recipeclient.model.RecipeStep
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetRecipeSteps(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetRecipeSteps::class.java)

    fun execute(param: GetRecipeStepsParam): Result<List<RecipeStep>, AppError> {
        logger.debug("Fetching steps for recipe id={}", param.recipeId)
        val steps = jdbi.withHandle<List<RecipeStep>, Exception> { handle ->
            handle.createQuery("SELECT ${RecipeStepRowAdapter.COLUMNS} FROM recipe_steps WHERE recipe_id = :recipeId ORDER BY position")
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> RecipeStepRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(steps)
    }
}
