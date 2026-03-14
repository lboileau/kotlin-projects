package com.acme.clients.logbookclient.model

import java.time.Instant
import java.util.UUID

data class LogBookJournalEntry(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val pageNumber: Int,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
