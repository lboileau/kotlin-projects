package com.acme.services.camperservice.config

import com.acme.clients.userclient.api.UserClient
import com.acme.clients.userclient.createUserClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserClientConfig {
    @Bean
    fun userClient(): UserClient = createUserClient()
}
