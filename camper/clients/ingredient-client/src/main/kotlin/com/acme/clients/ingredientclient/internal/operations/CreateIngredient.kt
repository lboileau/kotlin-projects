package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.CreateIngredientParam
import com.acme.clients.ingredientclient.internal.validations.ValidateCreateIngredient
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateIngredient(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateIngredient::class.java)
    private val validate = ValidateCreateIngredient()

    fun execute(param: CreateIngredientParam): Result<Ingredient, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating ingredient name={} category={}", param.name, param.category)
        return try {
            val entity = jdbi.withHandle<Ingredient, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO ingredients (id, name, category, default_unit, created_at, updated_at)
                    VALUES (:id, :name, :category, :defaultUnit, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("name", param.name)
                    .bind("category", param.category)
                    .bind("defaultUnit", param.defaultUnit)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                Ingredient(
                    id = id,
                    name = param.name,
                    category = param.category,
                    defaultUnit = param.defaultUnit,
                    createdAt = now,
                    updatedAt = now
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_ingredients_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Ingredient", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
