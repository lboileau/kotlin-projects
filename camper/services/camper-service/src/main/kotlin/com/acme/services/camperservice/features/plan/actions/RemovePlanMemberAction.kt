package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.GetByPlanIdAndUserIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.RemoveMemberParam
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.params.RemovePlanMemberParam
import com.acme.services.camperservice.features.plan.validations.ValidateRemovePlanMember
import org.slf4j.LoggerFactory

internal class RemovePlanMemberAction(
    private val planClient: PlanClient,
    private val invitationClient: InvitationClient,
    private val userClient: UserClient
) {
    private val logger = LoggerFactory.getLogger(RemovePlanMemberAction::class.java)
    private val validate = ValidateRemovePlanMember()

    fun execute(param: RemovePlanMemberParam): Result<Unit, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Authorization: self-remove, owner can remove anyone, inviter can cancel their own invite
        val isSelfRemove = param.userId == param.requestingUserId
        if (!isSelfRemove) {
            val plan = planClient.getById(GetByIdParam(param.planId))
            if (plan is Result.Failure) return Result.Failure(PlanError.fromClientError(plan.error))
            val isOwner = (plan as Result.Success).value.ownerId == param.requestingUserId

            if (!isOwner) {
                // Check if the requesting user is the inviter AND the target is still pending (no username)
                val invitation = invitationClient.getByPlanIdAndUserId(GetByPlanIdAndUserIdParam(param.planId, param.userId))
                val isInviter = invitation is Result.Success && invitation.value?.inviterId == param.requestingUserId
                val targetUser = userClient.getById(UserGetByIdParam(param.userId))
                val isPending = targetUser is Result.Success && targetUser.value.username == null
                if (!isInviter || !isPending) {
                    return Result.Failure(PlanError.NotOwner(param.planId.toString(), param.requestingUserId.toString()))
                }
            }
        }

        logger.debug("Removing user={} from plan={}", param.userId, param.planId)
        return when (val result = planClient.removeMember(RemoveMemberParam(planId = param.planId, userId = param.userId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
