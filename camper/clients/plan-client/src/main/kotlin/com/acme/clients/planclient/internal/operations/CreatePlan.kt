package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.CreatePlanParam
import com.acme.clients.planclient.internal.validations.ValidateCreatePlan
import com.acme.clients.planclient.model.Plan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreatePlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreatePlan::class.java)
    private val validate = ValidateCreatePlan()

    fun execute(param: CreatePlanParam): Result<Plan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating plan name={}", param.name)
        val entity = jdbi.withHandle<Plan, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at)
                VALUES (:id, :name, :visibility, :ownerId, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("name", param.name)
                .bind("visibility", param.visibility)
                .bind("ownerId", param.ownerId)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            Plan(id = id, name = param.name, visibility = param.visibility, ownerId = param.ownerId, createdAt = now, updatedAt = now)
        }
        return success(entity)
    }
}
