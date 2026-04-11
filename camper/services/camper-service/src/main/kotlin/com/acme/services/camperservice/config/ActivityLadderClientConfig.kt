package com.acme.services.camperservice.config

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.createActivityLadderClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ActivityLadderClientConfig {
    @Bean
    fun activityLadderClient(): ActivityLadderClient = createActivityLadderClient()
}
