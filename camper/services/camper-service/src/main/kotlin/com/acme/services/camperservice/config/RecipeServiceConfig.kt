package com.acme.services.camperservice.config

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.services.camperservice.features.recipe.service.RecipeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RecipeServiceConfig {
    @Bean
    fun recipeService(
        recipeClient: RecipeClient,
        ingredientClient: IngredientClient,
        recipeScraperClient: RecipeScraperClient
    ): RecipeService = RecipeService(recipeClient, ingredientClient, recipeScraperClient)
}
