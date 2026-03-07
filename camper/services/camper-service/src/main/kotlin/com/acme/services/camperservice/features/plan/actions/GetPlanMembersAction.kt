package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.plan.params.GetPlanMembersParam
import com.acme.services.camperservice.features.plan.validations.ValidateGetPlanMembers

internal class GetPlanMembersAction(private val planClient: PlanClient) {
    private val validate = ValidateGetPlanMembers()

    fun execute(param: GetPlanMembersParam): Result<List<PlanMember>, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
