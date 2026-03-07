package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.AddMemberParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.CreatePlanParam as ClientCreatePlanParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.CreatePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateCreatePlan
import org.slf4j.LoggerFactory

internal class CreatePlanAction(private val planClient: PlanClient) {
    private val logger = LoggerFactory.getLogger(CreatePlanAction::class.java)
    private val validate = ValidateCreatePlan()

    fun execute(param: CreatePlanParam): Result<Plan, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating plan name={} for user={}", param.name, param.userId)
        return when (val result = planClient.create(ClientCreatePlanParam(name = param.name, visibility = "private", ownerId = param.userId))) {
            is Result.Success -> {
                // Auto-add creator as member
                planClient.addMember(AddMemberParam(planId = result.value.id, userId = param.userId))
                Result.Success(PlanMapper.fromClient(result.value))
            }
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
