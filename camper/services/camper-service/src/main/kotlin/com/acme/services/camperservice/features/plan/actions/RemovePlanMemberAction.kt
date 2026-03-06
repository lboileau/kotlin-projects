package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.RemovePlanMemberParam
import com.acme.services.camperservice.features.plan.validations.ValidateRemovePlanMember

internal class RemovePlanMemberAction(private val planClient: PlanClient) {
    private val validate = ValidateRemovePlanMember()

    fun execute(param: RemovePlanMemberParam): Result<Unit, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
