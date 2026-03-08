package com.acme.services.camperservice.features.webhook.actions

import com.acme.clients.common.Result
import com.acme.clients.invitationclient.api.GetByResendEmailIdParam
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.api.UpdateStatusParam
import com.acme.services.camperservice.features.webhook.dto.ResendWebhookEvent
import org.slf4j.LoggerFactory

class HandleResendWebhookAction(
    private val invitationClient: InvitationClient
) {
    private val logger = LoggerFactory.getLogger(HandleResendWebhookAction::class.java)

    private val eventTypeToStatus = mapOf(
        "email.sent" to "sent",
        "email.delivered" to "delivered",
        "email.bounced" to "bounced",
        "email.delivery_delayed" to "delayed",
        "email.complained" to "complained"
    )

    fun execute(event: ResendWebhookEvent) {
        val status = eventTypeToStatus[event.type]
        if (status == null) {
            logger.debug("Ignoring unsupported webhook event type={}", event.type)
            return
        }

        val emailId = event.data.emailId
        logger.info("Processing webhook event type={} emailId={}", event.type, emailId)

        val invitation = when (val result = invitationClient.getByResendEmailId(GetByResendEmailIdParam(emailId))) {
            is Result.Success -> result.value
            is Result.Failure -> {
                logger.warn("Failed to look up invitation for emailId={}: {}", emailId, result.error.message)
                return
            }
        }

        if (invitation == null) {
            logger.debug("No invitation found for emailId={}, ignoring", emailId)
            return
        }

        when (val result = invitationClient.updateStatus(UpdateStatusParam(id = invitation.id, status = status))) {
            is Result.Success -> logger.info("Updated invitation id={} status={}", invitation.id, status)
            is Result.Failure -> logger.warn("Failed to update invitation id={}: {}", invitation.id, result.error.message)
        }
    }
}
