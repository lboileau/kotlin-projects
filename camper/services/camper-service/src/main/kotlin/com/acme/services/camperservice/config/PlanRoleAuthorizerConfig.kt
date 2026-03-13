package com.acme.services.camperservice.config

import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlanRoleAuthorizerConfig {
    @Bean
    fun planRoleAuthorizer(planClient: PlanClient): PlanRoleAuthorizer = PlanRoleAuthorizer(planClient)
}
