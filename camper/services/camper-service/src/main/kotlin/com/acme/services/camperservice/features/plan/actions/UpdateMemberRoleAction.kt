package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.services.camperservice.features.plan.error.PlanError
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

    /** Update a plan member's role. Implementation provided in a later PR. */
    fun execute(param: UpdateMemberRoleParam): Result<PlanMember, PlanError> {
        TODO("UpdateMemberRoleAction implementation comes in the service implementation PR")
    }
}
