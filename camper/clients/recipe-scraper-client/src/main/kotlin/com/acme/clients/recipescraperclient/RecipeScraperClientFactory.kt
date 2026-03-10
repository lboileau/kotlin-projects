package com.acme.clients.recipescraperclient

import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.internal.AnthropicRecipeScraperClient
import com.acme.clients.recipescraperclient.internal.NoOpRecipeScraperClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient

fun createRecipeScraperClient(): RecipeScraperClient {
    val apiKey = System.getProperty("ANTHROPIC_API_KEY")
        ?: System.getenv("ANTHROPIC_API_KEY")
        ?: throw IllegalStateException("ANTHROPIC_API_KEY must be set")

    val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    return AnthropicRecipeScraperClient(client)
}

fun createNoOpRecipeScraperClient(): RecipeScraperClient = NoOpRecipeScraperClient()
