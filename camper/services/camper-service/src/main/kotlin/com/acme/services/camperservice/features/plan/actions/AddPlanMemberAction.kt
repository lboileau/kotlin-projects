package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.api.SendEmailParam
import com.acme.clients.invitationclient.api.GetByPlanIdAndUserIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.api.UpdateStatusParam
import com.acme.clients.invitationclient.api.UpsertInvitationParam
import com.acme.clients.planclient.api.AddMemberParam
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.GetOrCreateUserParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.email.InviteEmailTemplate
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

    private val skipStatuses = setOf("sent", "delayed", "delivered", "complained")

    private val appBaseUrl: String by lazy {
        System.getProperty("APP_BASE_URL")
            ?: System.getenv("APP_BASE_URL")
            ?: "http://localhost:5173"
    }

    fun execute(param: AddPlanMemberParam): Result<PlanMember, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Get or create user by email
        val user = when (val result = userClient.getOrCreate(GetOrCreateUserParam(email = param.email))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(PlanError.Invalid("email", result.error.message))
        }

        logger.debug("Adding user={} to plan={}", user.id, param.planId)
        val member = when (val result = planClient.addMember(AddMemberParam(planId = param.planId, userId = user.id))) {
            is Result.Success -> PlanMapper.fromClient(result.value, user.username)
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(result.error))
        }

        // Check existing invitation status before upserting (dedup logic)
        val existingStatus = when (val result = invitationClient.getByPlanIdAndUserId(
            GetByPlanIdAndUserIdParam(planId = param.planId, userId = user.id)
        )) {
            is Result.Success -> result.value?.status
            is Result.Failure -> null
        }
        if (existingStatus != null && existingStatus in skipStatuses) {
            logger.debug("Skipping email send — invitation already has status={}", existingStatus)
            return Result.Success(member)
        }

        // Create or update invitation record in pending state
        val invitation = when (val result = invitationClient.upsert(
            UpsertInvitationParam(
                planId = param.planId,
                userId = user.id,
                email = param.email,
                inviterId = param.requestingUserId,
                resendEmailId = null,
                status = "pending"
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.warn("Failed to upsert invitation: {}", result.error.message)
                return Result.Success(member)
            }
        }

        // Fetch plan name and inviter name for the email
        val planName = when (val result = planClient.getById(GetByIdParam(param.planId))) {
            is Result.Success -> result.value.name
            is Result.Failure -> "the trip"
        }
        val inviterName = when (val result = userClient.getById(com.acme.clients.userclient.api.GetByIdParam(param.requestingUserId))) {
            is Result.Success -> result.value.username ?: result.value.email
            is Result.Failure -> "Someone"
        }

        // Send the invitation email
        val planUrl = "$appBaseUrl/plans/${param.planId}"
        val emailResult = emailClient.send(
            SendEmailParam(
                to = param.email,
                subject = InviteEmailTemplate.subject(planName),
                html = InviteEmailTemplate.html(inviterName, planName, planUrl)
            )
        )

        // Update invitation status based on send result
        when (emailResult) {
            is Result.Success -> {
                logger.info("Invite email sent to={} emailId={}", param.email, emailResult.value.emailId)
                invitationClient.updateStatus(
                    UpdateStatusParam(id = invitation.id, status = "sent", resendEmailId = emailResult.value.emailId)
                )
            }
            is Result.Failure -> {
                logger.warn("Failed to send invite email to={}: {}", param.email, emailResult.error.message)
                invitationClient.updateStatus(
                    UpdateStatusParam(id = invitation.id, status = "failed")
                )
            }
        }

        return Result.Success(member)
    }
}
