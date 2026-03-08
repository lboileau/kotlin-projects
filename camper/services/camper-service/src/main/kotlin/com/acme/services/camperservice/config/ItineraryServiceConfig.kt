package com.acme.services.camperservice.config

import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.itinerary.service.ItineraryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ItineraryServiceConfig {
    @Bean
    fun itineraryService(itineraryClient: ItineraryClient, planClient: PlanClient): ItineraryService =
        ItineraryService(itineraryClient, planClient)
}
