package com.acme.services.camperservice.config

import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserServiceConfig {
    @Bean
    fun userService(userClient: UserClient): UserService = UserService(userClient)
}
