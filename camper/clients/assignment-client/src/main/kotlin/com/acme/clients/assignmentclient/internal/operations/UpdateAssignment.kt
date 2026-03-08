package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.UpdateAssignmentParam
import com.acme.clients.assignmentclient.internal.validations.ValidateUpdateAssignment
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateAssignment(
    private val jdbi: Jdbi,
    private val getAssignmentById: GetAssignmentById
) {
    private val logger = LoggerFactory.getLogger(UpdateAssignment::class.java)
    private val validate = ValidateUpdateAssignment()

    fun execute(param: UpdateAssignmentParam): Result<Assignment, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating assignment id={}", param.id)
        return when (val existing = getAssignmentById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val updatedName = param.name ?: existing.value.name
                val updatedMaxOccupancy = param.maxOccupancy ?: existing.value.maxOccupancy
                val now = Instant.now()
                try {
                    jdbi.withHandle<Unit, Exception> { handle ->
                        handle.createUpdate(
                            """
                            UPDATE assignments SET name = :name, max_occupancy = :maxOccupancy, updated_at = :updatedAt
                            WHERE id = :id
                            """.trimIndent()
                        )
                            .bind("id", param.id)
                            .bind("name", updatedName)
                            .bind("maxOccupancy", updatedMaxOccupancy)
                            .bind("updatedAt", now)
                            .execute()
                    }
                    success(existing.value.copy(name = updatedName, maxOccupancy = updatedMaxOccupancy, updatedAt = now))
                } catch (e: Exception) {
                    if (e.message?.contains("uq_assignments_plan_id_name_type") == true || e.message?.contains("duplicate key") == true) {
                        failure(ConflictError("Assignment", "name '$updatedName' already exists for this plan and type"))
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
