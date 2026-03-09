package com.acme.services.camperservice.config

import com.acme.clients.assignmentclient.api.AssignmentClient
import com.acme.clients.gearsyncclient.api.GearSyncClient
import com.acme.clients.gearsyncclient.createGearSyncClient
import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.gearsync.service.GearSyncService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GearSyncServiceConfig {
    @Bean
    fun gearSyncClient(
        assignmentClient: AssignmentClient,
        itemClient: ItemClient,
        planClient: PlanClient,
    ): GearSyncClient = createGearSyncClient(assignmentClient, itemClient, planClient)

    @Bean
    fun gearSyncService(
        gearSyncClient: GearSyncClient,
    ): GearSyncService = GearSyncService(gearSyncClient)
}
