package com.acme.services.camperservice.config

import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.clients.logbookclient.createLogBookClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LogBookClientConfig {
    @Bean
    fun logBookClient(): LogBookClient = createLogBookClient()
}
