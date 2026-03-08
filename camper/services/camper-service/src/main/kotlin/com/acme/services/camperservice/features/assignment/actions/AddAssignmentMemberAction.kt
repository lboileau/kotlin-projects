package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetAssignmentMembersParam
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.AddAssignmentMemberParam as ClientAddAssignmentMemberParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.services.camperservice.features.assignment.dto.AssignmentMemberResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.AddAssignmentMemberParam
import org.slf4j.LoggerFactory

internal class AddAssignmentMemberAction(
    private val assignmentClient: AssignmentClient,
    private val userClient: UserClient
) {
    private val logger = LoggerFactory.getLogger(AddAssignmentMemberAction::class.java)

    fun execute(param: AddAssignmentMemberParam): Result<AssignmentMemberResponse, AssignmentError> {
        // Get assignment
        val assignment = when (val result = assignmentClient.getById(GetByIdParam(param.assignmentId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // Get current members
        val members = when (val result = assignmentClient.getMembers(GetAssignmentMembersParam(param.assignmentId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // Check capacity
        if (members.size >= assignment.maxOccupancy) {
            return Result.Failure(AssignmentError.AtCapacity(param.assignmentId.toString()))
        }

        logger.debug("Adding member userId={} to assignment={}", param.memberUserId, param.assignmentId)
        val member = when (val result = assignmentClient.addMember(
            ClientAddAssignmentMemberParam(
                assignmentId = param.assignmentId,
                userId = param.memberUserId,
                planId = assignment.planId,
                assignmentType = assignment.type
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> {
                val error = result.error
                if (error is ConflictError) {
                    return if (error.detail.contains("already_member")) {
                        Result.Failure(AssignmentError.AlreadyMember(param.assignmentId.toString(), param.memberUserId.toString()))
                    } else if (error.detail.contains("already_assigned_type")) {
                        Result.Failure(AssignmentError.AlreadyAssigned(param.memberUserId.toString(), assignment.type, assignment.planId.toString()))
                    } else {
                        Result.Failure(AssignmentError.fromClientError(error))
                    }
                }
                return Result.Failure(AssignmentError.fromClientError(error))
            }
        }

        // Get user for username enrichment
        val username = when (val userResult = userClient.getById(UserGetByIdParam(param.memberUserId))) {
            is Result.Success -> userResult.value.username
            is Result.Failure -> null
        }

        return Result.Success(AssignmentMapper.toResponse(AssignmentMapper.fromClient(member, username)))
    }
}
