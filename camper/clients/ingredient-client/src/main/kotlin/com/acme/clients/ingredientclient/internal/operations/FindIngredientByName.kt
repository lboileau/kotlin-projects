package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.FindByNameParam
import com.acme.clients.ingredientclient.internal.adapters.IngredientRowAdapter
import com.acme.clients.ingredientclient.internal.validations.ValidateFindIngredientByName
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class FindIngredientByName(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(FindIngredientByName::class.java)
    private val validate = ValidateFindIngredientByName()

    fun execute(param: FindByNameParam): Result<Ingredient?, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding ingredient by name={}", param.name)
        val entity = jdbi.withHandle<Ingredient?, Exception> { handle ->
            handle.createQuery(
                "SELECT id, name, category, default_unit, created_at, updated_at FROM ingredients WHERE LOWER(name) = LOWER(:name)"
            )
                .bind("name", param.name)
                .map { rs, _ -> IngredientRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return success(entity)
    }
}
