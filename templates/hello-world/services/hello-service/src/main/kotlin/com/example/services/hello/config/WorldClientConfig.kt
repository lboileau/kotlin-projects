package com.example.services.hello.config

import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.createWorldClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WorldClientConfig {
    @Bean
    fun worldClient(): WorldClient = createWorldClient()
}
