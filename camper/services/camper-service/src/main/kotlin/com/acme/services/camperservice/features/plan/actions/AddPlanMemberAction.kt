package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.plan.params.AddPlanMemberParam
import com.acme.services.camperservice.features.plan.validations.ValidateAddPlanMember

internal class AddPlanMemberAction(
    private val planClient: PlanClient,
    private val userClient: UserClient
) {
    private val validate = ValidateAddPlanMember()

    fun execute(param: AddPlanMemberParam): Result<PlanMember, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
