package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.UpdatePlanParam as ClientUpdatePlanParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.UpdatePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateUpdatePlan
import org.slf4j.LoggerFactory

internal class UpdatePlanAction(private val planClient: PlanClient) {
    private val logger = LoggerFactory.getLogger(UpdatePlanAction::class.java)
    private val validate = ValidateUpdatePlan()

    fun execute(param: UpdatePlanParam): Result<Plan, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Check ownership
        val existing = planClient.getById(GetByIdParam(param.planId))
        if (existing is Result.Failure) return Result.Failure(PlanError.fromClientError(existing.error))
        if ((existing as Result.Success).value.ownerId != param.userId) {
            return Result.Failure(PlanError.NotOwner(param.planId.toString(), param.userId.toString()))
        }

        logger.debug("Updating plan id={}", param.planId)
        return when (val result = planClient.update(ClientUpdatePlanParam(id = param.planId, name = param.name, visibility = param.visibility))) {
            is Result.Success -> Result.Success(PlanMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
