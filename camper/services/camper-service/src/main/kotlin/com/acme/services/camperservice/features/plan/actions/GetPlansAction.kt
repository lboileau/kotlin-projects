package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.GetPlansParam
import com.acme.services.camperservice.features.plan.validations.ValidateGetPlans

internal class GetPlansAction(private val planClient: PlanClient) {
    private val validate = ValidateGetPlans()

    fun execute(param: GetPlansParam): Result<List<Plan>, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
