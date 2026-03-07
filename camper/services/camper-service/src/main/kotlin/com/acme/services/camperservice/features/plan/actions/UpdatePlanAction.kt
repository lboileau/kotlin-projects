package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.UpdatePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateUpdatePlan

internal class UpdatePlanAction(private val planClient: PlanClient) {
    private val validate = ValidateUpdatePlan()

    fun execute(param: UpdatePlanParam): Result<Plan, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
