package com.acme.clients.gearpackclient

import com.acme.clients.gearpackclient.api.GearPackClient
import com.acme.clients.gearpackclient.internal.JdbiGearPackClient
import org.jdbi.v3.core.Jdbi

fun createGearPackClient(): GearPackClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiGearPackClient(jdbi)
}
