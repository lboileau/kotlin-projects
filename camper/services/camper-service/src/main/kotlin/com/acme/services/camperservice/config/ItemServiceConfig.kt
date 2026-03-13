package com.acme.services.camperservice.config

import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.item.service.ItemService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ItemServiceConfig {
    @Bean
    fun itemService(itemClient: ItemClient, planRoleAuthorizer: PlanRoleAuthorizer): ItemService =
        ItemService(itemClient, planRoleAuthorizer)
}
