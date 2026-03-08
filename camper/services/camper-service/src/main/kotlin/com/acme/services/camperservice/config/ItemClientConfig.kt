package com.acme.services.camperservice.config

import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.createItemClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ItemClientConfig {
    @Bean
    fun itemClient(): ItemClient = createItemClient()
}
