package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.RemoveMemberParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.RemovePlanMemberParam
import com.acme.services.camperservice.features.plan.validations.ValidateRemovePlanMember
import org.slf4j.LoggerFactory

internal class RemovePlanMemberAction(private val planClient: PlanClient) {
    private val logger = LoggerFactory.getLogger(RemovePlanMemberAction::class.java)
    private val validate = ValidateRemovePlanMember()

    fun execute(param: RemovePlanMemberParam): Result<Unit, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Authorization: self-remove or owner can remove anyone
        val isSelfRemove = param.userId == param.requestingUserId
        if (!isSelfRemove) {
            val plan = planClient.getById(GetByIdParam(param.planId))
            if (plan is Result.Failure) return Result.Failure(PlanError.fromClientError(plan.error))
            if ((plan as Result.Success).value.ownerId != param.requestingUserId) {
                return Result.Failure(PlanError.NotOwner(param.planId.toString(), param.requestingUserId.toString()))
            }
        }

        logger.debug("Removing user={} from plan={}", param.userId, param.planId)
        return when (val result = planClient.removeMember(RemoveMemberParam(planId = param.planId, userId = param.userId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
