package com.acmo.services.interviewservice.config

import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.clients.worldclient.createWorldClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldClientConfig {
    @Bean
    fun worldClient(): WorldClient = createWorldClient()
}
