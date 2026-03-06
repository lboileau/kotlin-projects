package com.acme.services.camperservice.config

import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.planclient.createPlanClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlanClientConfig {
    @Bean
    fun planClient(): PlanClient = createPlanClient()
}
