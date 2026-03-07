package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.DeletePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateDeletePlan

internal class DeletePlanAction(private val planClient: PlanClient) {
    private val validate = ValidateDeletePlan()

    fun execute(param: DeletePlanParam): Result<Unit, PlanError> {
        TODO("Implementation in service-impl PR")
    }
}
