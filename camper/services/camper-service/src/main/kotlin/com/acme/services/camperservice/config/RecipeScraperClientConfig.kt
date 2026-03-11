package com.acme.services.camperservice.config

import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.createNoOpRecipeScraperClient
import com.acme.clients.recipescraperclient.createRecipeScraperClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RecipeScraperClientConfig {
    private val logger = LoggerFactory.getLogger(RecipeScraperClientConfig::class.java)

    @Bean
    @ConditionalOnMissingBean
    fun recipeScraperClient(): RecipeScraperClient {
        val apiKey = System.getProperty("ANTHROPIC_API_KEY") ?: System.getenv("ANTHROPIC_API_KEY")
        if (apiKey.isNullOrBlank()) {
            logger.info("ANTHROPIC_API_KEY not set, using NoOpRecipeScraperClient")
            return createNoOpRecipeScraperClient()
        }
        logger.info("ANTHROPIC_API_KEY set, using Anthropic recipe scraper client")
        return createRecipeScraperClient()
    }
}
