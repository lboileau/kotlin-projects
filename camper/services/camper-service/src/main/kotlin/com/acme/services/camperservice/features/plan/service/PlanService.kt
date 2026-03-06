package com.acme.services.camperservice.features.plan.service

import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.actions.*
import com.acme.services.camperservice.features.plan.params.*

class PlanService(planClient: PlanClient, userClient: UserClient) {
    private val createPlan = CreatePlanAction(planClient)
    private val getPlans = GetPlansAction(planClient)
    private val updatePlan = UpdatePlanAction(planClient)
    private val deletePlan = DeletePlanAction(planClient)
    private val getPlanMembers = GetPlanMembersAction(planClient)
    private val addPlanMember = AddPlanMemberAction(planClient, userClient)
    private val removePlanMember = RemovePlanMemberAction(planClient)

    fun create(param: CreatePlanParam) = createPlan.execute(param)
    fun getPlans(param: GetPlansParam) = getPlans.execute(param)
    fun update(param: UpdatePlanParam) = updatePlan.execute(param)
    fun delete(param: DeletePlanParam) = deletePlan.execute(param)
    fun getMembers(param: GetPlanMembersParam) = getPlanMembers.execute(param)
    fun addMember(param: AddPlanMemberParam) = addPlanMember.execute(param)
    fun removeMember(param: RemovePlanMemberParam) = removePlanMember.execute(param)
}
