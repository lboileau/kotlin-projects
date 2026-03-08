package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.DeleteAssignmentParam
import com.acme.clients.assignmentclient.internal.validations.ValidateDeleteAssignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeleteAssignment(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeleteAssignment::class.java)
    private val validate = ValidateDeleteAssignment()

    fun execute(param: DeleteAssignmentParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting assignment id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM assignments WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("Assignment", param.id.toString()))
    }
}
