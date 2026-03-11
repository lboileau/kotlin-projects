package com.acme.clients.ingredientclient

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.ingredientclient.internal.JdbiIngredientClient
import org.jdbi.v3.core.Jdbi

fun createIngredientClient(): IngredientClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiIngredientClient(jdbi)
}
