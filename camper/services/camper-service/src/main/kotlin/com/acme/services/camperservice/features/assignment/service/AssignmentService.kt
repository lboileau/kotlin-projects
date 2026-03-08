package com.acme.services.camperservice.features.assignment.service

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.api.GetByIdParam
import com.acme.clients.common.Result
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.clients.gearsyncclient.api.SyncGearParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.assignment.actions.*
import com.acme.services.camperservice.features.assignment.params.*

class AssignmentService(
    private val assignmentClient: AssignmentClient,
    planClient: PlanClient,
    userClient: UserClient,
    private val gearSyncClient: GearSyncClient,
) {
    private val createAssignment = CreateAssignmentAction(assignmentClient, planClient)
    private val getAssignments = GetAssignmentsAction(assignmentClient)
    private val getAssignment = GetAssignmentAction(assignmentClient, userClient)
    private val updateAssignment = UpdateAssignmentAction(assignmentClient, planClient)
    private val deleteAssignment = DeleteAssignmentAction(assignmentClient, planClient)
    private val addAssignmentMember = AddAssignmentMemberAction(assignmentClient, userClient)
    private val removeAssignmentMember = RemoveAssignmentMemberAction(assignmentClient, planClient)
    private val transferOwnership = TransferOwnershipAction(assignmentClient, planClient)

    fun create(param: CreateAssignmentParam) = createAssignment.execute(param).also { result ->
        if (result is Result.Success) {
            gearSyncClient.sync(SyncGearParam(param.planId))
        }
    }

    fun getAssignments(param: GetAssignmentsParam) = getAssignments.execute(param)
    fun getAssignment(param: GetAssignmentParam) = getAssignment.execute(param)
    fun update(param: UpdateAssignmentParam) = updateAssignment.execute(param)

    fun delete(param: DeleteAssignmentParam): Result<Unit, com.acme.services.camperservice.features.assignment.error.AssignmentError> {
        // Resolve planId before deletion
        val planId = when (val result = assignmentClient.getById(GetByIdParam(param.assignmentId))) {
            is Result.Success -> result.value.planId
            is Result.Failure -> null
        }

        return deleteAssignment.execute(param).also { result ->
            if (result is Result.Success && planId != null) {
                gearSyncClient.sync(SyncGearParam(planId))
            }
        }
    }

    fun addMember(param: AddAssignmentMemberParam) = addAssignmentMember.execute(param)
    fun removeMember(param: RemoveAssignmentMemberParam) = removeAssignmentMember.execute(param)
    fun transferOwnership(param: TransferOwnershipParam) = transferOwnership.execute(param)
}
