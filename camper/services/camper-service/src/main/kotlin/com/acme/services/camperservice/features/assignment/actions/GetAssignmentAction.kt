package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetAssignmentMembersParam
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.services.camperservice.features.assignment.dto.AssignmentDetailResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.GetAssignmentParam
import org.slf4j.LoggerFactory

internal class GetAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val userClient: UserClient
) {
    private val logger = LoggerFactory.getLogger(GetAssignmentAction::class.java)

    fun execute(param: GetAssignmentParam): Result<AssignmentDetailResponse, AssignmentError> {
        logger.debug("Getting assignment id={}", param.assignmentId)

        // Get assignment
        val assignment = when (val result = assignmentClient.getById(GetByIdParam(param.assignmentId))) {
            is Result.Success -> AssignmentMapper.fromClient(result.value)
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // Get members
        val clientMembers = when (val result = assignmentClient.getMembers(GetAssignmentMembersParam(param.assignmentId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // Enrich members with username
        val members = clientMembers.map { clientMember ->
            val username = when (val userResult = userClient.getById(UserGetByIdParam(clientMember.userId))) {
                is Result.Success -> userResult.value.username
                is Result.Failure -> null
            }
            AssignmentMapper.fromClient(clientMember, username)
        }

        return Result.Success(AssignmentMapper.toDetailResponse(assignment, members))
    }
}
