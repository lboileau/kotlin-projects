package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.assignment.dto.AssignmentMemberResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.AddAssignmentMemberParam

internal class AddAssignmentMemberAction(private val assignmentClient: AssignmentClient) {
    fun execute(param: AddAssignmentMemberParam): Result<AssignmentMemberResponse, AssignmentError> = TODO()
}
