package com.acme.services.camperservice.config

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.services.camperservice.features.recipe.service.IngredientService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IngredientServiceConfig {
    @Bean
    fun ingredientService(ingredientClient: IngredientClient): IngredientService =
        IngredientService(ingredientClient)
}
