package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.assignmentclient.api.RemoveAssignmentMemberParam as ClientRemoveAssignmentMemberParam
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.GetByIdParam as PlanGetByIdParam
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.RemoveAssignmentMemberParam
import org.slf4j.LoggerFactory

internal class RemoveAssignmentMemberAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    private val logger = LoggerFactory.getLogger(RemoveAssignmentMemberAction::class.java)

    fun execute(param: RemoveAssignmentMemberParam): Result<Unit, AssignmentError> {
        // Get assignment
        val assignment = when (val result = assignmentClient.getById(GetByIdParam(param.assignmentId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(AssignmentError.fromClientError(result.error))
        }

        // If not self-remove, check authorization
        if (param.memberUserId != param.userId) {
            val plan = when (val result = planClient.getById(PlanGetByIdParam(assignment.planId))) {
                is Result.Success -> result.value
                is Result.Failure -> return Result.Failure(AssignmentError.PlanNotFound(assignment.planId.toString()))
            }

            if (assignment.ownerId != param.userId && plan.ownerId != param.userId) {
                return Result.Failure(AssignmentError.NotOwner(param.assignmentId.toString(), param.userId.toString()))
            }
        }

        logger.debug("Removing member userId={} from assignment={}", param.memberUserId, param.assignmentId)
        return when (val result = assignmentClient.removeMember(
            ClientRemoveAssignmentMemberParam(assignmentId = param.assignmentId, userId = param.memberUserId)
        )) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(AssignmentError.fromClientError(result.error))
        }
    }
}
