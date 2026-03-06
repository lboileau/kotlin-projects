package com.acme.services.camperservice.config

import com.acme.clients.worldclient.api.WorldClient
import com.acme.services.camperservice.features.world.service.WorldService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldServiceConfig {
    @Bean
    fun worldService(worldClient: WorldClient): WorldService = WorldService(worldClient)
}
