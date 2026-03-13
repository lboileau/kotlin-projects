package com.acme.clients.planclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember

/**
 * Client interface for Plan entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface PlanClient {
    /** Retrieve a plan by its unique identifier. */
    fun getById(param: GetByIdParam): Result<Plan, AppError>

    /** Retrieve all plans that a user is a member of. */
    fun getByUserId(param: GetByUserIdParam): Result<List<Plan>, AppError>

    /** Retrieve all public plans. */
    fun getPublicPlans(param: GetPublicPlansParam): Result<List<Plan>, AppError>

    /** Create a new plan. */
    fun create(param: CreatePlanParam): Result<Plan, AppError>

    /** Update an existing plan's name. */
    fun update(param: UpdatePlanParam): Result<Plan, AppError>

    /** Delete a plan by its unique identifier. */
    fun delete(param: DeletePlanParam): Result<Unit, AppError>

    /** Retrieve all members of a plan. */
    fun getMembers(param: GetMembersParam): Result<List<PlanMember>, AppError>

    /** Add a user as a member of a plan. */
    fun addMember(param: AddMemberParam): Result<PlanMember, AppError>

    /** Remove a user from a plan. */
    fun removeMember(param: RemoveMemberParam): Result<Unit, AppError>

    /** Update a member's role in a plan. */
    fun updateMemberRole(param: UpdateMemberRoleParam): Result<PlanMember, AppError>
}
