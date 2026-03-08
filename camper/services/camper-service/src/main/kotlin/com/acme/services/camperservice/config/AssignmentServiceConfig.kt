package com.acme.services.camperservice.config

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.assignment.service.AssignmentService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AssignmentServiceConfig {
    @Bean
    fun assignmentService(
        assignmentClient: AssignmentClient,
        planClient: PlanClient,
        userClient: UserClient,
    ): AssignmentService = AssignmentService(assignmentClient, planClient, userClient)
}
