package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.GetByPlanIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.GetMembersParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.api.UpdateMemberRoleParam as ClientUpdateMemberRoleParam
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.plan.params.UpdateMemberRoleParam
import com.acme.services.camperservice.features.plan.validations.ValidateUpdateMemberRole
import org.slf4j.LoggerFactory

internal class UpdateMemberRoleAction(
    private val planClient: PlanClient,
    private val userClient: UserClient,
    private val invitationClient: InvitationClient
) {
    private val logger = LoggerFactory.getLogger(UpdateMemberRoleAction::class.java)
    private val validate = ValidateUpdateMemberRole()

    fun execute(param: UpdateMemberRoleParam): Result<PlanMember, PlanError> {
        // 1. Validate role
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // 2. Verify the plan exists
        val plan = when (val planResult = planClient.getById(GetByIdParam(param.planId))) {
            is Result.Success -> planResult.value
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(planResult.error))
        }

        // 3. Verify the requesting user is the plan owner
        if (plan.ownerId != param.requestingUserId) {
            return Result.Failure(PlanError.NotOwner(param.planId.toString(), param.requestingUserId.toString()))
        }

        // 4. Verify the target user is not the owner
        if (param.userId == plan.ownerId) {
            return Result.Failure(PlanError.CannotChangeOwnerRole(param.planId.toString()))
        }

        // 5. Verify the target user is a plan member
        val members = when (val membersResult = planClient.getMembers(GetMembersParam(param.planId))) {
            is Result.Success -> membersResult.value
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(membersResult.error))
        }
        if (members.none { it.userId == param.userId }) {
            return Result.Failure(PlanError.NotMember(param.planId.toString(), param.userId.toString()))
        }

        // 6. Update the role
        logger.debug("Updating role for userId={} in plan={} to role={}", param.userId, param.planId, param.role)
        val updatedClientMember = when (val result = planClient.updateMemberRole(
            ClientUpdateMemberRoleParam(planId = param.planId, userId = param.userId, role = param.role)
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(result.error))
        }

        // 7. Enrich with username, email, invitation status
        val user = when (val userResult = userClient.getById(UserGetByIdParam(param.userId))) {
            is Result.Success -> userResult.value
            is Result.Failure -> null
        }
        val invitations = when (val invResult = invitationClient.getByPlanId(GetByPlanIdParam(param.planId))) {
            is Result.Success -> invResult.value.associateBy { it.userId }
            is Result.Failure -> emptyMap()
        }
        val invitation = invitations[param.userId]

        return Result.Success(
            PlanMapper.fromClient(
                updatedClientMember,
                username = user?.username,
                email = user?.email ?: invitation?.email,
                invitationStatus = invitation?.status
            )
        )
    }
}
