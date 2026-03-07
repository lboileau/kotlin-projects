package com.acme.clients.planclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.planclient.api.*
import com.acme.clients.planclient.model.Plan
import com.acme.clients.planclient.model.PlanMember

class FakePlanClient : PlanClient {
    override fun getById(param: GetByIdParam): Result<Plan, AppError> =
        throw NotImplementedError("FakePlanClient.getById")

    override fun getByUserId(param: GetByUserIdParam): Result<List<Plan>, AppError> =
        throw NotImplementedError("FakePlanClient.getByUserId")

    override fun getPublicPlans(param: GetPublicPlansParam): Result<List<Plan>, AppError> =
        throw NotImplementedError("FakePlanClient.getPublicPlans")

    override fun create(param: CreatePlanParam): Result<Plan, AppError> =
        throw NotImplementedError("FakePlanClient.create")

    override fun update(param: UpdatePlanParam): Result<Plan, AppError> =
        throw NotImplementedError("FakePlanClient.update")

    override fun delete(param: DeletePlanParam): Result<Unit, AppError> =
        throw NotImplementedError("FakePlanClient.delete")

    override fun getMembers(param: GetMembersParam): Result<List<PlanMember>, AppError> =
        throw NotImplementedError("FakePlanClient.getMembers")

    override fun addMember(param: AddMemberParam): Result<PlanMember, AppError> =
        throw NotImplementedError("FakePlanClient.addMember")

    override fun removeMember(param: RemoveMemberParam): Result<Unit, AppError> =
        throw NotImplementedError("FakePlanClient.removeMember")
}
