package com.acme.services.camperservice.config

import com.acme.services.camperservice.features.mealplan.service.MealPlanService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MealPlanServiceConfig {
    @Bean
    fun mealPlanService(): MealPlanService = MealPlanService()
}
