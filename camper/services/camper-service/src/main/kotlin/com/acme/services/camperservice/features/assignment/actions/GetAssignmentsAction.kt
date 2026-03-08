package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByPlanIdParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.mapper.AssignmentMapper
import com.acme.services.camperservice.features.assignment.params.GetAssignmentsParam
import org.slf4j.LoggerFactory

internal class GetAssignmentsAction(private val assignmentClient: AssignmentClient) {
    private val logger = LoggerFactory.getLogger(GetAssignmentsAction::class.java)

    fun execute(param: GetAssignmentsParam): Result<List<AssignmentResponse>, AssignmentError> {
        logger.debug("Getting assignments for plan={} type={}", param.planId, param.type)
        return when (val result = assignmentClient.getByPlanId(GetByPlanIdParam(planId = param.planId, type = param.type))) {
            is Result.Success -> Result.Success(result.value.map { AssignmentMapper.toResponse(AssignmentMapper.fromClient(it)) })
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
