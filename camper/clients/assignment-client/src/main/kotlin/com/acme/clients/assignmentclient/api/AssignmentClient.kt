package com.acme.clients.assignmentclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.assignmentclient.model.Assignment
import com.acme.clients.assignmentclient.model.AssignmentMember

/**
 * Client interface for Assignment entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface AssignmentClient {
    /** Create a new assignment within a plan. */
    fun create(param: CreateAssignmentParam): Result<Assignment, AppError>

    /** Retrieve an assignment by its unique identifier. */
    fun getById(param: GetByIdParam): Result<Assignment, AppError>

    /** Retrieve all assignments for a plan, optionally filtered by type. */
    fun getByPlanId(param: GetByPlanIdParam): Result<List<Assignment>, AppError>

    /** Update an existing assignment. Null fields are left unchanged. */
    fun update(param: UpdateAssignmentParam): Result<Assignment, AppError>

    /** Delete an assignment by its unique identifier. */
    fun delete(param: DeleteAssignmentParam): Result<Unit, AppError>

    /** Add a user as a member of an assignment. */
    fun addMember(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError>

    /** Remove a user from an assignment. */
    fun removeMember(param: RemoveAssignmentMemberParam): Result<Unit, AppError>

    /** Retrieve all members of an assignment. */
    fun getMembers(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError>

    /** Transfer ownership of an assignment to a new owner. */
    fun transferOwnership(param: TransferOwnershipParam): Result<Assignment, AppError>
}
