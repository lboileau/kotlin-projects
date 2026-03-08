package com.acme.clients.invitationclient

import com.acme.clients.invitationclient.api.InvitationClient
import com.acme.clients.invitationclient.internal.JdbiInvitationClient
import org.jdbi.v3.core.Jdbi

fun createInvitationClient(): InvitationClient {
    val url = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5433/camper_db"
    val user = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "postgres"
    val password = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "postgres"
    val jdbi = Jdbi.create(url, user, password)
    return JdbiInvitationClient(jdbi)
}
