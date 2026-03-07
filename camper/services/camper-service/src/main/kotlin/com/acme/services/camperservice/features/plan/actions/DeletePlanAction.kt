package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.DeletePlanParam as ClientDeletePlanParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.DeletePlanParam
import com.acme.services.camperservice.features.plan.validations.ValidateDeletePlan
import org.slf4j.LoggerFactory

internal class DeletePlanAction(private val planClient: PlanClient) {
    private val logger = LoggerFactory.getLogger(DeletePlanAction::class.java)
    private val validate = ValidateDeletePlan()

    fun execute(param: DeletePlanParam): Result<Unit, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Check ownership
        val existing = planClient.getById(GetByIdParam(param.planId))
        if (existing is Result.Failure) return Result.Failure(PlanError.fromClientError(existing.error))
        if ((existing as Result.Success).value.ownerId != param.userId) {
            return Result.Failure(PlanError.NotOwner(param.planId.toString(), param.userId.toString()))
        }

        logger.debug("Deleting plan id={}", param.planId)
        return when (val result = planClient.delete(ClientDeletePlanParam(id = param.planId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
