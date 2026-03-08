package com.acme.services.camperservice.config

import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.plan.service.PlanService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlanServiceConfig {
    @Bean
    fun planService(
        planClient: PlanClient,
        userClient: UserClient,
        emailClient: EmailClient,
        invitationClient: InvitationClient
    ): PlanService = PlanService(planClient, userClient, emailClient, invitationClient)
}
