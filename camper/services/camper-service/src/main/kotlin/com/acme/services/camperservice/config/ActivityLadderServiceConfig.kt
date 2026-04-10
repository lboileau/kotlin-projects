package com.acme.services.camperservice.config

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.activityladder.service.ActivityLadderService
import com.acme.services.camperservice.websocket.LadderEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ActivityLadderServiceConfig {
    @Bean
    fun activityLadderService(
        ladderClient: ActivityLadderClient,
        userClient: UserClient,
        eventPublisher: LadderEventPublisher,
    ): ActivityLadderService = ActivityLadderService(ladderClient, userClient, eventPublisher)
}
