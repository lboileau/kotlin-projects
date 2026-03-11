package com.acme.clients.recipeclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipeclient.api.CreateRecipeParam
import com.acme.clients.recipeclient.internal.validations.ValidateCreateRecipe
import com.acme.clients.recipeclient.model.Recipe
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateRecipe(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateRecipe::class.java)
    private val validate = ValidateCreateRecipe()

    fun execute(param: CreateRecipeParam): Result<Recipe, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating recipe name={} status={}", param.name, param.status)
        return try {
            val entity = jdbi.withHandle<Recipe, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO recipes (id, name, description, web_link, base_servings, status, created_by, created_at, updated_at)
                    VALUES (:id, :name, :description, :webLink, :baseServings, :status, :createdBy, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("name", param.name)
                    .bind("description", param.description)
                    .bind("webLink", param.webLink)
                    .bind("baseServings", param.baseServings)
                    .bind("status", param.status)
                    .bind("createdBy", param.createdBy)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                Recipe(
                    id = id,
                    name = param.name,
                    description = param.description,
                    webLink = param.webLink,
                    baseServings = param.baseServings,
                    status = param.status,
                    createdBy = param.createdBy,
                    duplicateOfId = null,
                    createdAt = now,
                    updatedAt = now
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_recipes_web_link") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Recipe", "web_link '${param.webLink}' already exists"))
            } else {
                throw e
            }
        }
    }
}
