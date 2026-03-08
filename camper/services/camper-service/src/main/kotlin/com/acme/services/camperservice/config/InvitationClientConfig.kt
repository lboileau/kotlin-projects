package com.acme.services.camperservice.config

import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.createInvitationClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InvitationClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun invitationClient(): InvitationClient = createInvitationClient()
}
