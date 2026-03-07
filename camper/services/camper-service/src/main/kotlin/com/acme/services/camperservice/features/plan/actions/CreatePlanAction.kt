package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.CreatePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateCreatePlan

internal class CreatePlanAction(private val planClient: PlanClient) {
    private val validate = ValidateCreatePlan()

    fun execute(param: CreatePlanParam): Result<Plan, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
