package com.acme.clients.assignmentclient.internal

import com.acme.clients.assignmentclient.api.*
import com.acme.clients.assignmentclient.internal.operations.*
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.assignmentclient.model.AssignmentMember
import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import org.jdbi.v3.core.Jdbi

internal class JdbiAssignmentClient(jdbi: Jdbi) : AssignmentClient {

    private val createAssignment = CreateAssignment(jdbi)
    private val getAssignmentById = GetAssignmentById(jdbi)
    private val getAssignmentsByPlanId = GetAssignmentsByPlanId(jdbi)
    private val updateAssignment = UpdateAssignment(jdbi, getAssignmentById)
    private val deleteAssignment = DeleteAssignment(jdbi)
    private val addAssignmentMember = AddAssignmentMember(jdbi)
    private val removeAssignmentMember = RemoveAssignmentMember(jdbi)
    private val getAssignmentMembers = GetAssignmentMembers(jdbi)
    private val transferOwnershipOp = TransferOwnership(jdbi, getAssignmentById)

    override fun create(param: CreateAssignmentParam): Result<Assignment, AppError> = createAssignment.execute(param)
    override fun getById(param: GetByIdParam): Result<Assignment, AppError> = getAssignmentById.execute(param)
    override fun getByPlanId(param: GetByPlanIdParam): Result<List<Assignment>, AppError> = getAssignmentsByPlanId.execute(param)
    override fun update(param: UpdateAssignmentParam): Result<Assignment, AppError> = updateAssignment.execute(param)
    override fun delete(param: DeleteAssignmentParam): Result<Unit, AppError> = deleteAssignment.execute(param)
    override fun addMember(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError> = addAssignmentMember.execute(param)
    override fun removeMember(param: RemoveAssignmentMemberParam): Result<Unit, AppError> = removeAssignmentMember.execute(param)
    override fun getMembers(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError> = getAssignmentMembers.execute(param)
    override fun transferOwnership(param: TransferOwnershipParam): Result<Assignment, AppError> = transferOwnershipOp.execute(param)
}
