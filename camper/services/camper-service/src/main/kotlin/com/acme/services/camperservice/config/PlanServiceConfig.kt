package com.acme.services.camperservice.config

import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.service.PlanService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlanServiceConfig {
    @Bean
    fun planService(planClient: PlanClient, userClient: UserClient): PlanService = PlanService(planClient, userClient)
}
