package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.RemoveAssignmentMemberParam
import com.acme.clients.assignmentclient.internal.validations.ValidateRemoveAssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemoveAssignmentMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveAssignmentMember::class.java)
    private val validate = ValidateRemoveAssignmentMember()

    fun execute(param: RemoveAssignmentMemberParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing member userId={} from assignment id={}", param.userId, param.assignmentId)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM assignment_members WHERE assignment_id = :assignmentId AND user_id = :userId")
                .bind("assignmentId", param.assignmentId)
                .bind("userId", param.userId)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("AssignmentMember", "${param.assignmentId}/${param.userId}"))
    }
}
