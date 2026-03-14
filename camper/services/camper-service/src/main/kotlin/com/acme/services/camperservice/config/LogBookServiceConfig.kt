package com.acme.services.camperservice.config

import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.service.LogBookService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LogBookServiceConfig {
    @Bean
    fun logBookService(logBookClient: LogBookClient, planRoleAuthorizer: PlanRoleAuthorizer): LogBookService =
        LogBookService(logBookClient, planRoleAuthorizer)
}
