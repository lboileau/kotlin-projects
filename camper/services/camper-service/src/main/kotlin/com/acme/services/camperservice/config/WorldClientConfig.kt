package com.acme.services.camperservice.config

import com.acme.clients.worldclient.api.WorldClient
import com.acme.clients.worldclient.createWorldClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldClientConfig {
    @Bean
    fun worldClient(): WorldClient = createWorldClient()
}
