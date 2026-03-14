package com.acme.clients.logbookclient.model

import java.time.Instant
import java.util.UUID

data class LogBookFaq(
    val id: UUID,
    val planId: UUID,
    val question: String,
    val askedById: UUID,
    val answer: String?,
    val answeredById: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
