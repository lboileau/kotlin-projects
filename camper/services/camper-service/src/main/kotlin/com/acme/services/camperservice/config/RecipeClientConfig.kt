package com.acme.services.camperservice.config

import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipeclient.createRecipeClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RecipeClientConfig {
    @Bean
    @ConditionalOnMissingBean
    fun recipeClient(): RecipeClient = createRecipeClient()
}
