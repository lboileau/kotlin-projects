package com.acme.clients.ingredientclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.ingredientclient.api.CreateBatchParam
import com.acme.clients.ingredientclient.internal.validations.ValidateCreateIngredientBatch
import com.acme.clients.ingredientclient.model.Ingredient
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateIngredientBatch(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateIngredientBatch::class.java)
    private val validate = ValidateCreateIngredientBatch()

    fun execute(param: CreateBatchParam): Result<List<Ingredient>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating ingredient batch count={}", param.ingredients.size)
        return try {
            val entities = jdbi.inTransaction<List<Ingredient>, Exception> { handle ->
                param.ingredients.map { ing ->
                    val id = UUID.randomUUID()
                    val now = Instant.now()
                    handle.createUpdate(
                        """
                        INSERT INTO ingredients (id, name, category, default_unit, created_at, updated_at)
                        VALUES (:id, :name, :category, :defaultUnit, :createdAt, :updatedAt)
                        """.trimIndent()
                    )
                        .bind("id", id)
                        .bind("name", ing.name)
                        .bind("category", ing.category)
                        .bind("defaultUnit", ing.defaultUnit)
                        .bind("createdAt", now)
                        .bind("updatedAt", now)
                        .execute()
                    Ingredient(
                        id = id,
                        name = ing.name,
                        category = ing.category,
                        defaultUnit = ing.defaultUnit,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
            success(entities)
        } catch (e: Exception) {
            if (e.message?.contains("uq_ingredients_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Ingredient", "one or more ingredient names already exist"))
            } else {
                throw e
            }
        }
    }
}
