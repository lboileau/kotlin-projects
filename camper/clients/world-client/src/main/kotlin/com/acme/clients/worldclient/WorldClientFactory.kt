package com.acme.clients.worldclient

import com.acme.clients.worldclient.api.WorldClient
import com.acme.clients.worldclient.internal.JdbiWorldClient
import org.jdbi.v3.core.Jdbi

fun createWorldClient(): WorldClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiWorldClient(jdbi)
}
