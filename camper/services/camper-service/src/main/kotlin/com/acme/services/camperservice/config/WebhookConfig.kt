package com.acme.services.camperservice.config

import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.services.camperservice.features.webhook.actions.HandleResendWebhookAction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebhookConfig {
    @Bean
    fun handleResendWebhookAction(invitationClient: InvitationClient): HandleResendWebhookAction =
        HandleResendWebhookAction(invitationClient)
}
