package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.assignment.dto.AssignmentResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.GetAssignmentsParam

internal class GetAssignmentsAction(private val assignmentClient: AssignmentClient) {
    fun execute(param: GetAssignmentsParam): Result<List<AssignmentResponse>, AssignmentError> = TODO()
}
