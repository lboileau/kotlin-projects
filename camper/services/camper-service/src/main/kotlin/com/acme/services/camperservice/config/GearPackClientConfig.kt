package com.acme.services.camperservice.config

import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.createGearPackClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GearPackClientConfig {
    @Bean
    fun gearPackClient(): GearPackClient = createGearPackClient()
}
