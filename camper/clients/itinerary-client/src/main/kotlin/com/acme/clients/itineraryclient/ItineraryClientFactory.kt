package com.acme.clients.itineraryclient

import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.itineraryclient.internal.JdbiItineraryClient
import org.jdbi.v3.core.Jdbi

fun createItineraryClient(): ItineraryClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiItineraryClient(jdbi)
}
