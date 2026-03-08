package com.acme.clients.assignmentclient.fake

import com.acme.clients.assignmentclient.api.*
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError

class FakeAssignmentClient : AssignmentClient {

    override fun create(param: CreateAssignmentParam): Result<Assignment, AppError> {
        throw NotImplementedError("stub")
    }

    override fun getById(param: GetByIdParam): Result<Assignment, AppError> {
        throw NotImplementedError("stub")
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Assignment>, AppError> {
        throw NotImplementedError("stub")
    }

    override fun update(param: UpdateAssignmentParam): Result<Assignment, AppError> {
        throw NotImplementedError("stub")
    }

    override fun delete(param: DeleteAssignmentParam): Result<Unit, AppError> {
        throw NotImplementedError("stub")
    }

    override fun addMember(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError> {
        throw NotImplementedError("stub")
    }

    override fun removeMember(param: RemoveAssignmentMemberParam): Result<Unit, AppError> {
        throw NotImplementedError("stub")
    }

    override fun getMembers(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError> {
        throw NotImplementedError("stub")
    }

    override fun transferOwnership(param: TransferOwnershipParam): Result<Assignment, AppError> {
        throw NotImplementedError("stub")
    }

    fun reset() {
        // No-op for stub implementation
    }
}
