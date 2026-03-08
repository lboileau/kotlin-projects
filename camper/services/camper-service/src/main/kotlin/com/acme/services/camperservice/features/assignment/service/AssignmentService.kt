package com.acme.services.camperservice.features.assignment.service

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.assignment.actions.*
import com.acme.services.camperservice.features.assignment.params.*

class AssignmentService(
    assignmentClient: AssignmentClient,
    planClient: PlanClient,
    userClient: UserClient
) {
    private val createAssignment = CreateAssignmentAction(assignmentClient, planClient)
    private val getAssignments = GetAssignmentsAction(assignmentClient)
    private val getAssignment = GetAssignmentAction(assignmentClient, userClient)
    private val updateAssignment = UpdateAssignmentAction(assignmentClient, planClient)
    private val deleteAssignment = DeleteAssignmentAction(assignmentClient, planClient)
    private val addAssignmentMember = AddAssignmentMemberAction(assignmentClient, userClient)
    private val removeAssignmentMember = RemoveAssignmentMemberAction(assignmentClient, planClient)
    private val transferOwnership = TransferOwnershipAction(assignmentClient, planClient)

    fun create(param: CreateAssignmentParam) = createAssignment.execute(param)
    fun getAssignments(param: GetAssignmentsParam) = getAssignments.execute(param)
    fun getAssignment(param: GetAssignmentParam) = getAssignment.execute(param)
    fun update(param: UpdateAssignmentParam) = updateAssignment.execute(param)
    fun delete(param: DeleteAssignmentParam) = deleteAssignment.execute(param)
    fun addMember(param: AddAssignmentMemberParam) = addAssignmentMember.execute(param)
    fun removeMember(param: RemoveAssignmentMemberParam) = removeAssignmentMember.execute(param)
    fun transferOwnership(param: TransferOwnershipParam) = transferOwnership.execute(param)
}
