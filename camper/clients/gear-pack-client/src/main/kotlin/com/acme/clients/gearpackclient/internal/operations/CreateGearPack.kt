package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.gearpackclient.api.CreateGearPackParam
import com.acme.clients.gearpackclient.internal.validations.ValidateCreateGearPack
import com.acme.clients.gearpackclient.model.GearPack
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateGearPack(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateGearPack::class.java)
    private val validate = ValidateCreateGearPack()

    fun execute(param: CreateGearPackParam): Result<GearPack, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating gear pack name={}", param.name)
        return try {
            val entity = jdbi.withHandle<GearPack, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO gear_packs (id, name, description, created_by, created_at, updated_at)
                    VALUES (:id, :name, :description, :createdBy, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("name", param.name)
                    .bind("description", param.description)
                    .bind("createdBy", param.createdBy)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                GearPack(
                    id = id,
                    name = param.name,
                    description = param.description,
                    items = emptyList(),
                    createdBy = param.createdBy,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_gear_packs_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("GearPack", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
