package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.GetByPlanIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.planclient.api.GetMembersParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.GetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.plan.params.GetPlanMembersParam
import com.acme.services.camperservice.features.plan.validations.ValidateGetPlanMembers
import org.slf4j.LoggerFactory

internal class GetPlanMembersAction(
    private val planClient: PlanClient,
    private val userClient: UserClient,
    private val invitationClient: InvitationClient
) {
    private val logger = LoggerFactory.getLogger(GetPlanMembersAction::class.java)
    private val validate = ValidateGetPlanMembers()

    fun execute(param: GetPlanMembersParam): Result<List<PlanMember>, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting members for plan id={}", param.planId)
        return when (val result = planClient.getMembers(GetMembersParam(param.planId))) {
            is Result.Success -> {
                // Fetch invitations for the plan to enrich members with status
                val invitations = when (val invResult = invitationClient.getByPlanId(GetByPlanIdParam(param.planId))) {
                    is Result.Success -> invResult.value.associateBy { it.userId }
                    is Result.Failure -> emptyMap()
                }

                val members = result.value.map { clientMember ->
                    val user = when (val userResult = userClient.getById(GetByIdParam(clientMember.userId))) {
                        is Result.Success -> userResult.value
                        is Result.Failure -> null
                    }
                    val invitation = invitations[clientMember.userId]
                    PlanMapper.fromClient(
                        clientMember,
                        username = user?.username,
                        email = user?.email ?: invitation?.email,
                        invitationStatus = invitation?.status,
                        invitedBy = invitation?.inviterId,
                        avatarSeed = user?.avatarSeed
                    )
                }
                Result.Success(members)
            }
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }
    }
}
