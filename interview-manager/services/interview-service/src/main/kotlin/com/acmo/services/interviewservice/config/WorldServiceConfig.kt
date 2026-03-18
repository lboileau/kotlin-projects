package com.acmo.services.interviewservice.config

import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.services.interviewservice.features.world.service.WorldService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldServiceConfig {
    @Bean
    fun worldService(worldClient: WorldClient): WorldService = WorldService(worldClient)
}
