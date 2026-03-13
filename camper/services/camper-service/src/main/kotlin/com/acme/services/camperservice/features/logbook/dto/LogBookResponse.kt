package com.acme.services.camperservice.features.logbook.dto

import java.time.Instant
import java.util.UUID

data class LogBookFaqResponse(
    val id: UUID,
    val planId: UUID,
    val question: String,
    val askedById: UUID,
    val answer: String?,
    val answeredById: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LogBookJournalEntryResponse(
    val id: UUID,
    val planId: UUID,
    val userId: UUID,
    val pageNumber: Int,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
