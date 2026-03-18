package com.acmo.clients.worldclient.internal.operations

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.common.error.ConflictError
import com.acmo.clients.common.failure
import com.acmo.clients.common.success
import com.acmo.clients.worldclient.api.GetByIdParam
import com.acmo.clients.worldclient.api.UpdateWorldParam
import com.acmo.clients.worldclient.internal.validations.ValidateUpdateWorld
import com.acmo.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateWorld(
    private val jdbi: Jdbi,
    private val getWorldById: GetWorldById
) {
    private val logger = LoggerFactory.getLogger(UpdateWorld::class.java)
    private val validate = ValidateUpdateWorld()

    fun execute(param: UpdateWorldParam): Result<World, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        return when (val existing = getWorldById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val updatedName = param.name ?: existing.value.name
                val updatedGreeting = param.greeting ?: existing.value.greeting
                val now = Instant.now()
                logger.debug("Updating world id={}", param.id)
                try {
                    jdbi.withHandle<Unit, Exception> { handle ->
                        handle.createUpdate(
                            """
                            UPDATE worlds SET name = :name, greeting = :greeting, updated_at = :updatedAt
                            WHERE id = :id
                            """.trimIndent()
                        )
                            .bind("id", param.id)
                            .bind("name", updatedName)
                            .bind("greeting", updatedGreeting)
                            .bind("updatedAt", now)
                            .execute()
                    }
                    success(existing.value.copy(name = updatedName, greeting = updatedGreeting, updatedAt = now))
                } catch (e: Exception) {
                    if (e.message?.contains("uq_worlds_name") == true || e.message?.contains("duplicate key") == true) {
                        failure(ConflictError("World", "name '$updatedName' already exists"))
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
