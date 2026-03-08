package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.CreateAssignmentParam as ClientCreateAssignmentParam
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.CreateAssignmentParam
import org.slf4j.LoggerFactory

internal class CreateAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(CreateAssignmentAction::class.java)

    fun execute(param: CreateAssignmentParam): Result<AssignmentResponse, AssignmentError> {
        // Validate name not blank
        if (param.name.isBlank()) {
            return Result.Failure(AssignmentError.Invalid("name", "must not be blank"))
        }

        // Validate type must be 'tent' or 'canoe'
        if (param.type != "tent" && param.type != "canoe") {
            return Result.Failure(AssignmentError.Invalid("type", "must be 'tent' or 'canoe'"))
        }

        // Default maxOccupancy if null
        val maxOccupancy = param.maxOccupancy ?: if (param.type == "tent") 4 else 2

        // Validate plan exists
        when (val planResult = planClient.getById(PlanGetByIdParam(param.planId))) {
            is Result.Failure -> return Result.Failure(AssignmentError.PlanNotFound(param.planId.toString()))
            is Result.Success -> {} // plan exists
        }

        logger.debug("Creating assignment name={} type={} for plan={}", param.name, param.type, param.planId)
        return when (val result = assignmentClient.create(
            ClientCreateAssignmentParam(
                planId = param.planId,
                name = param.name,
                type = param.type,
                maxOccupancy = maxOccupancy,
                ownerId = param.userId
            )
        )) {
            is Result.Success -> Result.Success(AssignmentMapper.toResponse(AssignmentMapper.fromClient(result.value)))
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
