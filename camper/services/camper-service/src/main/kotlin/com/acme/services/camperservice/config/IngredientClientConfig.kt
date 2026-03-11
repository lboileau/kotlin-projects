package com.acme.services.camperservice.config

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.createIngredientClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IngredientClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun ingredientClient(): IngredientClient = createIngredientClient()
}
