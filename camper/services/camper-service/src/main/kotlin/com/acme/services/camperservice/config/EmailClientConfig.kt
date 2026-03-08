package com.acme.services.camperservice.config

import com.acme.clients.emailclient.api.EmailClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmailClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun emailClient(): EmailClient {
        // TODO: Wire real Resend client or fake based on RESEND_API_KEY in client-impl PR
        throw IllegalStateException("No EmailClient bean configured. Set RESEND_API_KEY or provide a test configuration.")
    }
}
