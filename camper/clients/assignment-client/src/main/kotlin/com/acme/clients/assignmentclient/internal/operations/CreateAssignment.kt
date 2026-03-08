package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.CreateAssignmentParam
import com.acme.clients.assignmentclient.internal.validations.ValidateCreateAssignment
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateAssignment(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateAssignment::class.java)
    private val validate = ValidateCreateAssignment()

    fun execute(param: CreateAssignmentParam): Result<Assignment, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating assignment name={} type={} for plan={}", param.name, param.type, param.planId)
        return try {
            val entity = jdbi.withHandle<Assignment, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO assignments (id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at)
                    VALUES (:id, :planId, :name, :type, :maxOccupancy, :ownerId, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("planId", param.planId)
                    .bind("name", param.name)
                    .bind("type", param.type)
                    .bind("maxOccupancy", param.maxOccupancy)
                    .bind("ownerId", param.ownerId)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                Assignment(
                    id = id,
                    planId = param.planId,
                    name = param.name,
                    type = param.type,
                    maxOccupancy = param.maxOccupancy,
                    ownerId = param.ownerId,
                    createdAt = now,
                    updatedAt = now
                )
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_assignments_plan_id_name_type") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Assignment", "name '${param.name}' already exists for this plan and type"))
            } else {
                throw e
            }
        }
    }
}
