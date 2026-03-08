package com.acme.clients.assignmentclient.internal.operations

import com.acme.clients.assignmentclient.api.GetAssignmentMembersParam
import com.acme.clients.assignmentclient.internal.adapters.AssignmentMemberRowAdapter
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetAssignmentMembers(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAssignmentMembers::class.java)

    fun execute(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError> {
        logger.debug("Finding members for assignment id={}", param.assignmentId)
        val entities = jdbi.withHandle<List<AssignmentMember>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT assignment_id, user_id, plan_id, assignment_type, created_at
                FROM assignment_members
                WHERE assignment_id = :assignmentId
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("assignmentId", param.assignmentId)
                .map { rs, _ -> AssignmentMemberRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
