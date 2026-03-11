package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.ingredientclient.api.GetByIdParam
import com.acme.clients.ingredientclient.api.UpdateIngredientParam
import com.acme.clients.ingredientclient.internal.adapters.IngredientRowAdapter
import com.acme.clients.ingredientclient.internal.validations.ValidateUpdateIngredient
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateIngredient(
    private val jdbi: Jdbi,
    private val getIngredientById: GetIngredientById
) {
    private val logger = LoggerFactory.getLogger(UpdateIngredient::class.java)
    private val validate = ValidateUpdateIngredient()

    fun execute(param: UpdateIngredientParam): Result<Ingredient, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = getIngredientById.execute(GetByIdParam(param.id))
        if (existing is Result.Failure) return existing

        logger.debug("Updating ingredient id={}", param.id)
        return try {
            val sets = mutableListOf<String>()
            if (param.name != null) sets.add("name = :name")
            if (param.category != null) sets.add("category = :category")
            if (param.defaultUnit != null) sets.add("default_unit = :defaultUnit")
            sets.add("updated_at = :updatedAt")

            val entity = jdbi.withHandle<Ingredient, Exception> { handle ->
                val now = Instant.now()
                handle.createUpdate("UPDATE ingredients SET ${sets.joinToString(", ")} WHERE id = :id")
                    .bind("id", param.id)
                    .also { q -> if (param.name != null) q.bind("name", param.name) }
                    .also { q -> if (param.category != null) q.bind("category", param.category) }
                    .also { q -> if (param.defaultUnit != null) q.bind("defaultUnit", param.defaultUnit) }
                    .bind("updatedAt", now)
                    .execute()

                handle.createQuery(
                    "SELECT id, name, category, default_unit, created_at, updated_at FROM ingredients WHERE id = :id"
                )
                    .bind("id", param.id)
                    .map { rs, _ -> IngredientRowAdapter.fromResultSet(rs) }
                    .one()
            }
            Result.Success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_ingredients_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Ingredient", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
