package com.acmo.clients.worldclient.internal.operations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.ConflictError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.CreateWorldParam
import com.acmo.clients.worldclient.internal.validations.ValidateCreateWorld
import com.acmo.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateWorld(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateWorld::class.java)
    private val validate = ValidateCreateWorld()

    fun execute(param: CreateWorldParam): Result<World, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating world name={}", param.name)
        return try {
            val entity = jdbi.withHandle<World, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO worlds (id, name, greeting, created_at, updated_at)
                    VALUES (:id, :name, :greeting, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("name", param.name)
                    .bind("greeting", param.greeting)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                World(id = id, name = param.name, greeting = param.greeting, createdAt = now, updatedAt = now)
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_worlds_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("World", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
