package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.planclient.api.AddMemberParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.GetOrCreateUserParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.PlanMember
import com.acme.services.camperservice.features.plan.params.AddPlanMemberParam
import com.acme.services.camperservice.features.plan.validations.ValidateAddPlanMember
import org.slf4j.LoggerFactory

internal class AddPlanMemberAction(
    private val planClient: PlanClient,
    private val userClient: UserClient,
    private val emailClient: EmailClient,
    private val invitationClient: InvitationClient
) {
    private val logger = LoggerFactory.getLogger(AddPlanMemberAction::class.java)
    private val validate = ValidateAddPlanMember()

    fun execute(param: AddPlanMemberParam): Result<PlanMember, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Get or create user by email
        val user = when (val result = userClient.getOrCreate(GetOrCreateUserParam(email = param.email))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(PlanError.Invalid("email", result.error.message))
        }

        logger.debug("Adding user={} to plan={}", user.id, param.planId)
        return when (val result = planClient.addMember(AddMemberParam(planId = param.planId, userId = user.id))) {
            is Result.Success -> Result.Success(PlanMapper.fromClient(result.value, user.username))
            is Result.Failure -> Result.Failure(PlanError.fromClientError(result.error))
        }

        // TODO: Invitation email sending will be implemented in the service-impl PR
    }
}
