package com.acme.services.camperservice.config

import com.acme.clients.invitationclient.api.InvitationClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InvitationClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun invitationClient(): InvitationClient {
        // TODO: Wire real JDBI client in client-impl PR
        throw IllegalStateException("No InvitationClient bean configured. Provide a proper configuration.")
    }
}
