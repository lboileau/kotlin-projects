package com.acme.services.camperservice.features.assignment.actions

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.assignment.dto.AssignmentDetailResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.assignment.params.GetAssignmentParam

internal class GetAssignmentAction(
    private val assignmentClient: AssignmentClient,
    private val userClient: UserClient
) {
    fun execute(param: GetAssignmentParam): Result<AssignmentDetailResponse, AssignmentError> = TODO()
}
