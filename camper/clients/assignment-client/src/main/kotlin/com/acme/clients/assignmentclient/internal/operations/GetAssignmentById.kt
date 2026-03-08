package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.internal.adapters.AssignmentRowAdapter
import com.acme.clients.assignmentclient.internal.validations.ValidateGetAssignmentById
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAssignmentById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAssignmentById::class.java)
    private val validate = ValidateGetAssignmentById()

    fun execute(param: GetByIdParam): Result<Assignment, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding assignment by id={}", param.id)
        val entity = jdbi.withHandle<Assignment?, Exception> { handle ->
            handle.createQuery("SELECT id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at FROM assignments WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> AssignmentRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Assignment", param.id.toString()))
    }
}
