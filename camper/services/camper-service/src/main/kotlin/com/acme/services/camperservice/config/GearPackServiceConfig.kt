package com.acme.services.camperservice.config

import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.gearpack.service.GearPackService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GearPackServiceConfig {
    @Bean
    fun gearPackService(
        gearPackClient: GearPackClient,
        itemClient: ItemClient,
        planRoleAuthorizer: PlanRoleAuthorizer,
    ): GearPackService = GearPackService(gearPackClient, itemClient, planRoleAuthorizer)
}
