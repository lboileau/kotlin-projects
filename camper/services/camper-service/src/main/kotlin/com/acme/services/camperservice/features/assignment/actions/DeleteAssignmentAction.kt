package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.DeleteAssignmentParam as ClientDeleteAssignmentParam
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.DeleteAssignmentParam
import org.slf4j.LoggerFactory

internal class DeleteAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(DeleteAssignmentAction::class.java)

    fun execute(param: DeleteAssignmentParam): Result<Unit, AssignmentError> {
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

        logger.debug("Deleting assignment id={}", param.assignmentId)
        return when (val result = assignmentClient.delete(ClientDeleteAssignmentParam(param.assignmentId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
