package com.acme.services.camperservice.config

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.assignmentclient.createAssignmentClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AssignmentClientConfig {
    @Bean
    fun assignmentClient(): AssignmentClient = createAssignmentClient()
}
