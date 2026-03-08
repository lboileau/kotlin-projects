package com.acme.clients.assignmentclient

import com.acme.clients.assignmentclient.api.*
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError

fun createAssignmentClient(): AssignmentClient = object : AssignmentClient {
    private fun <T> notImplemented(): Result<T, AppError> =
        Result.Failure(NotFoundError("AssignmentClient", "not-implemented"))

    override fun create(param: CreateAssignmentParam) = notImplemented<Assignment>()
    override fun getById(param: GetByIdParam) = notImplemented<Assignment>()
    override fun getByPlanId(param: GetByPlanIdParam) = notImplemented<List<Assignment>>()
    override fun update(param: UpdateAssignmentParam) = notImplemented<Assignment>()
    override fun delete(param: DeleteAssignmentParam) = notImplemented<Unit>()
    override fun addMember(param: AddAssignmentMemberParam) = notImplemented<AssignmentMember>()
    override fun removeMember(param: RemoveAssignmentMemberParam) = notImplemented<Unit>()
    override fun getMembers(param: GetAssignmentMembersParam) = notImplemented<List<AssignmentMember>>()
    override fun transferOwnership(param: TransferOwnershipParam) = notImplemented<Assignment>()
}
