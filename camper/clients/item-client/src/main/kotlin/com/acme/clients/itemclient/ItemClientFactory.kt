package com.acme.clients.itemclient

import com.acme.clients.itemclient.api.ItemClient
import com.acme.clients.itemclient.internal.JdbiItemClient
import org.jdbi.v3.core.Jdbi

fun createItemClient(): ItemClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiItemClient(jdbi)
}
