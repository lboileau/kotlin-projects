package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.UpdateAssignmentParam as ClientUpdateAssignmentParam
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.UpdateAssignmentParam
import org.slf4j.LoggerFactory

internal class UpdateAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(UpdateAssignmentAction::class.java)

    fun execute(param: UpdateAssignmentParam): Result<AssignmentResponse, AssignmentError> {
        // Validate: if name provided, not blank
        if (param.name != null && param.name.isBlank()) {
            return Result.Failure(AssignmentError.Invalid("name", "must not be blank"))
        }

        // Validate: if maxOccupancy provided, > 0
        if (param.maxOccupancy != null && param.maxOccupancy <= 0) {
            return Result.Failure(AssignmentError.Invalid("maxOccupancy", "must be greater than 0"))
        }

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

        logger.debug("Updating assignment id={}", param.assignmentId)
        return when (val result = assignmentClient.update(
            ClientUpdateAssignmentParam(
                id = param.assignmentId,
                name = param.name,
                maxOccupancy = param.maxOccupancy
            )
        )) {
            is Result.Success -> Result.Success(AssignmentMapper.toResponse(AssignmentMapper.fromClient(result.value)))
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
