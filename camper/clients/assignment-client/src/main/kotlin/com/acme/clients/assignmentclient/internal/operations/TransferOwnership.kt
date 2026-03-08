package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.TransferOwnershipParam
import com.acme.clients.assignmentclient.internal.validations.ValidateTransferOwnership
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class TransferOwnership(
    private val jdbi: Jdbi,
    private val getAssignmentById: GetAssignmentById
) {
    private val logger = LoggerFactory.getLogger(TransferOwnership::class.java)
    private val validate = ValidateTransferOwnership()

    fun execute(param: TransferOwnershipParam): Result<Assignment, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Transferring ownership of assignment id={} to user={}", param.id, param.newOwnerId)
        return when (val existing = getAssignmentById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val now = Instant.now()
                jdbi.withHandle<Unit, Exception> { handle ->
                    handle.createUpdate(
                        """
                        UPDATE assignments SET owner_id = :ownerId, updated_at = :updatedAt
                        WHERE id = :id
                        """.trimIndent()
                    )
                        .bind("id", param.id)
                        .bind("ownerId", param.newOwnerId)
                        .bind("updatedAt", now)
                        .execute()
                }
                success(existing.value.copy(ownerId = param.newOwnerId, updatedAt = now))
            }
        }
    }
}
