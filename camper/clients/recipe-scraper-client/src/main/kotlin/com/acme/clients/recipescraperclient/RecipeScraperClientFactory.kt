package com.acme.clients.recipescraperclient

import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.internal.NoOpRecipeScraperClient

fun createRecipeScraperClient(): RecipeScraperClient {
    System.getProperty("ANTHROPIC_API_KEY")
        ?: System.getenv("ANTHROPIC_API_KEY")
        ?: throw IllegalStateException("ANTHROPIC_API_KEY must be set")

    throw NotImplementedError("AnthropicRecipeScraperClient not yet implemented")
}

fun createNoOpRecipeScraperClient(): RecipeScraperClient = NoOpRecipeScraperClient()
