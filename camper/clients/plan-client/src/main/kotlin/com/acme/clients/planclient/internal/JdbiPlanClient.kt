package com.acme.clients.planclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.planclient.api.*
import com.acme.clients.planclient.internal.operations.*
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember
import org.jdbi.v3.core.Jdbi

internal class JdbiPlanClient(jdbi: Jdbi) : PlanClient {

    private val getPlanById = GetPlanById(jdbi)
    private val getPlansByUserId = GetPlansByUserId(jdbi)
    private val getPublicPlansOp = GetPublicPlans(jdbi)
    private val createPlan = CreatePlan(jdbi)
    private val updatePlan = UpdatePlan(jdbi, getPlanById)
    private val deletePlan = DeletePlan(jdbi)
    private val getPlanMembers = GetPlanMembers(jdbi)
    private val addPlanMember = AddPlanMember(jdbi)
    private val removePlanMember = RemovePlanMember(jdbi)

    override fun getById(param: GetByIdParam): Result<Plan, AppError> = getPlanById.execute(param)
    override fun getByUserId(param: GetByUserIdParam): Result<List<Plan>, AppError> = getPlansByUserId.execute(param)
    override fun getPublicPlans(param: GetPublicPlansParam): Result<List<Plan>, AppError> = getPublicPlansOp.execute(param)
    override fun create(param: CreatePlanParam): Result<Plan, AppError> = createPlan.execute(param)
    override fun update(param: UpdatePlanParam): Result<Plan, AppError> = updatePlan.execute(param)
    override fun delete(param: DeletePlanParam): Result<Unit, AppError> = deletePlan.execute(param)
    override fun getMembers(param: GetMembersParam): Result<List<PlanMember>, AppError> = getPlanMembers.execute(param)
    override fun addMember(param: AddMemberParam): Result<PlanMember, AppError> = addPlanMember.execute(param)
    override fun removeMember(param: RemoveMemberParam): Result<Unit, AppError> = removePlanMember.execute(param)
    override fun updateMemberRole(param: UpdateMemberRoleParam): Result<PlanMember, AppError> =
        throw NotImplementedError("updateMemberRole operation not yet implemented")
}
