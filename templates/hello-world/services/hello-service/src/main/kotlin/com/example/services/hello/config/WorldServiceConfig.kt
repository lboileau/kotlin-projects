package com.example.services.hello.config

import com.example.clients.worldclient.api.WorldClient
import com.example.services.hello.features.world.service.WorldService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldServiceConfig {
    @Bean
    fun worldService(worldClient: WorldClient): WorldService = WorldService(worldClient)
}
