package com.acme.services.camperservice.config

import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.mealplanclient.createMealPlanClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MealPlanClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun mealPlanClient(): MealPlanClient = createMealPlanClient()
}
