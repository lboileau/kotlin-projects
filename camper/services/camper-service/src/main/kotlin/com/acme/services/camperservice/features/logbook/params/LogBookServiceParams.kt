package com.acme.services.camperservice.features.logbook.params

import java.util.UUID

data class AskFaqParam(
    val planId: UUID,
    val question: String,
    val requestingUserId: UUID,
)

data class AnswerFaqParam(
    val faqId: UUID,
    val planId: UUID,
    val answer: String,
    val requestingUserId: UUID,
)

data class GetFaqsParam(
    val planId: UUID,
    val requestingUserId: UUID,
)

data class DeleteFaqParam(
    val faqId: UUID,
    val planId: UUID,
    val requestingUserId: UUID,
)

data class CreateJournalEntryParam(
    val planId: UUID,
    val content: String,
    val requestingUserId: UUID,
)

data class UpdateJournalEntryParam(
    val entryId: UUID,
    val planId: UUID,
    val content: String,
    val requestingUserId: UUID,
)

data class GetJournalEntriesParam(
    val planId: UUID,
    val requestingUserId: UUID,
)

data class DeleteJournalEntryParam(
    val entryId: UUID,
    val planId: UUID,
    val requestingUserId: UUID,
)
