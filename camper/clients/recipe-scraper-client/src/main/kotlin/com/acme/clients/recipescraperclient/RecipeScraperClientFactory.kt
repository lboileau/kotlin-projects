package com.acme.clients.recipescraperclient

import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.internal.AnthropicRecipeScraperClient
import com.acme.clients.recipescraperclient.internal.NoOpRecipeScraperClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient

/**
 * Env vars:
 * - ANTHROPIC_API_KEY (required)
 * - RECIPE_SCRAPER_MODEL (optional) — model id, defaults to [AnthropicRecipeScraperClient.DEFAULT_MODEL]
 */
fun createRecipeScraperClient(): RecipeScraperClient {
    val apiKey = System.getProperty("ANTHROPIC_API_KEY")
        ?: System.getenv("ANTHROPIC_API_KEY")
        ?: throw IllegalStateException("ANTHROPIC_API_KEY must be set")
    val model = (System.getProperty("RECIPE_SCRAPER_MODEL") ?: System.getenv("RECIPE_SCRAPER_MODEL"))
        ?.takeIf { it.isNotBlank() }
        ?: AnthropicRecipeScraperClient.DEFAULT_MODEL

    val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    return AnthropicRecipeScraperClient(client, model)
}

fun createNoOpRecipeScraperClient(): RecipeScraperClient = NoOpRecipeScraperClient()
