package com.acme.services.camperservice.config

import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.createEmailClient
import com.acme.clients.emailclient.createNoOpEmailClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmailClientConfig {
    private val logger = LoggerFactory.getLogger(EmailClientConfig::class.java)

    @Bean
    @ConditionalOnMissingBean
    fun emailClient(): EmailClient {
        val apiKey = System.getProperty("RESEND_API_KEY") ?: System.getenv("RESEND_API_KEY")
        if (apiKey.isNullOrBlank()) {
            logger.info("RESEND_API_KEY not set, using NoOpEmailClient (no emails will be sent)")
            return createNoOpEmailClient()
        }
        logger.info("RESEND_API_KEY set, using Resend email client")
        return createEmailClient()
    }
}
