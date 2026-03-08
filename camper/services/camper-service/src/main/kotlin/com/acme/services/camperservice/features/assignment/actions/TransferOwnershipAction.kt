package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.TransferOwnershipParam as ClientTransferOwnershipParam
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.TransferOwnershipParam
import org.slf4j.LoggerFactory

internal class TransferOwnershipAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(TransferOwnershipAction::class.java)

    fun execute(param: TransferOwnershipParam): Result<AssignmentResponse, AssignmentError> {
        // Get assignment
        val assignment = when (val result = assignmentClient.getById(GetByIdParam(param.assignmentId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // Get plan
        val plan = when (val result = planClient.getById(PlanGetByIdParam(assignment.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.PlanNotFound(assignment.planId.toString()))
        }

        // Check requester is assignment owner OR plan owner
        if (assignment.ownerId != param.userId && plan.ownerId != param.userId) {
            return Result.Failure(AssignmentError.NotOwner(param.assignmentId.toString(), param.userId.toString()))
        }

        logger.debug("Transferring ownership of assignment={} to user={}", param.assignmentId, param.newOwnerId)
        return when (val result = assignmentClient.transferOwnership(
            ClientTransferOwnershipParam(id = param.assignmentId, newOwnerId = param.newOwnerId)
        )) {
            is Result.Success -> Result.Success(AssignmentMapper.toResponse(AssignmentMapper.fromClient(result.value)))
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
