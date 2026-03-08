package com.acme.services.camperservice.config

import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.createItineraryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ItineraryClientConfig {
    @Bean
    fun itineraryClient(): ItineraryClient = createItineraryClient()
}
