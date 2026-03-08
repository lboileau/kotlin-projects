package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.DeleteAssignmentParam

internal class DeleteAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val planClient: PlanClient
) {
    fun execute(param: DeleteAssignmentParam): Result<Unit, AssignmentError> = TODO()
}
