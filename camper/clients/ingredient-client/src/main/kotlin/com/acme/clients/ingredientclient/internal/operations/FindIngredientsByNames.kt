package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.FindByNamesParam
import com.acme.clients.ingredientclient.internal.adapters.IngredientRowAdapter
import com.acme.clients.ingredientclient.internal.validations.ValidateFindIngredientsByNames
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class FindIngredientsByNames(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(FindIngredientsByNames::class.java)
    private val validate = ValidateFindIngredientsByNames()

    fun execute(param: FindByNamesParam): Result<List<Ingredient>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.names.isEmpty()) return success(emptyList())

        logger.debug("Finding ingredients by names count={}", param.names.size)
        val entities = jdbi.withHandle<List<Ingredient>, Exception> { handle ->
            handle.createQuery(
                "SELECT id, name, category, default_unit, created_at, updated_at FROM ingredients WHERE LOWER(name) IN (<names>) ORDER BY name"
            )
                .bindList("names", param.names.map { it.lowercase() })
                .map { rs, _ -> IngredientRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
