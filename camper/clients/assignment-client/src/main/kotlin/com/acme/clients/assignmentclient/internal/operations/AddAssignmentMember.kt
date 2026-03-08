package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.AddAssignmentMemberParam
import com.acme.clients.assignmentclient.internal.validations.ValidateAddAssignmentMember
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class AddAssignmentMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddAssignmentMember::class.java)
    private val validate = ValidateAddAssignmentMember()

    fun execute(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding member userId={} to assignment id={}", param.userId, param.assignmentId)
        return try {
            val now = Instant.now()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(
                    """
                    INSERT INTO assignment_members (assignment_id, user_id, plan_id, assignment_type, created_at)
                    VALUES (:assignmentId, :userId, :planId, :assignmentType, :createdAt)
                    """.trimIndent()
                )
                    .bind("assignmentId", param.assignmentId)
                    .bind("userId", param.userId)
                    .bind("planId", param.planId)
                    .bind("assignmentType", param.assignmentType)
                    .bind("createdAt", now)
                    .execute()
            }
            success(
                AssignmentMember(
                    assignmentId = param.assignmentId,
                    userId = param.userId,
                    planId = param.planId,
                    assignmentType = param.assignmentType,
                    createdAt = now
                )
            )
        } catch (e: Exception) {
            when {
                e.message?.contains("assignment_members_pkey") == true ||
                    (e.message?.contains("duplicate key") == true && e.message?.contains("assignment_id") == true && e.message?.contains("user_id") == true) ->
                    failure(ConflictError("AssignmentMember", "already_member"))
                e.message?.contains("uq_assignment_members_plan_id_user_id_type") == true ->
                    failure(ConflictError("AssignmentMember", "already_assigned_type"))
                else -> throw e
            }
        }
    }
}
