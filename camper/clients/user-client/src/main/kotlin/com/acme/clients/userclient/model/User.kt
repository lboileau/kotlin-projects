package com.acme.clients.userclient.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
