package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.internal.adapters.IngredientRowAdapter
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAllIngredients(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAllIngredients::class.java)

    fun execute(): Result<List<Ingredient>, AppError> {
        logger.debug("Fetching all ingredients")
        val entities = jdbi.withHandle<List<Ingredient>, Exception> { handle ->
            handle.createQuery(
                "SELECT id, name, category, default_unit, created_at, updated_at FROM ingredients ORDER BY name"
            )
                .map { rs, _ -> IngredientRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
